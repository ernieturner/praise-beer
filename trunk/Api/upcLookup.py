#!/usr/bin/env python

# -----------------------------------------------------------------------------
# Imports
# -----------------------------------------------------------------------------
import wsgiref.handlers
import sys
import xmlrpclib
import logging
from StringIO import StringIO
import urllib
import re
import PBUtils
import PBDatabase

from google.appengine.api import urlfetch
from google.appengine.ext import webapp
from google.appengine.ext.webapp.util import run_wsgi_app
from django.utils import simplejson
from google.appengine.ext import db
from google.appengine.api import memcache

# -----------------------------------------------------------------------------
# Constants
# -----------------------------------------------------------------------------

UPC_DATABASE_RPC_URL = 'http://www.upcdatabase.com/rpc'
INVALID_UPC_CODE = 0;
NO_UPC_CODE_SENT = 1;
NO_UPC_FOUND = 2;
NO_BEER_FOUND = 3;
RATING_LOOKUP_TIMEOUT = 4;

# -----------------------------------------------------------------------------
# Custom RPC Transport Class
# -----------------------------------------------------------------------------

class GoogleXMLRPCTransport(object):
    """Handles an HTTP transaction to an XML-RPC server."""

    def __init__(self, use_datetime=0):
        self._use_datetime = use_datetime

    def request(self, host, handler, request_body, verbose=0):
        # issue XML-RPC request

        result = None
        url = 'http://%s%s' % (host, handler)
        try:
            response = urlfetch.fetch(url,
                                      payload=request_body,
                                      method=urlfetch.POST,
                                      headers={'Content-Type': 'text/xml'})
        except:
            msg = 'Failed to fetch %s' % url
            raise xmlrpclib.ProtocolError(host + handler, 500, msg, {})
                                          
        if response.status_code != 200:
            logging.error('%s returned status code %s' % 
                          (url, response.status_code))
            raise xmlrpclib.ProtocolError(host + handler,
                                          response.status_code,
                                          "",
                                          response.headers)
        else:
            result = self.__parse_response(response.content)
        
        return result

    def __parse_response(self, response_body):
        p, u = xmlrpclib.getparser(use_datetime=self._use_datetime)
        p.feed(response_body)
        return u.close()


					
# -----------------------------------------------------------------------------
# Web Request Handler Class
# -----------------------------------------------------------------------------

class MainHandler(webapp.RequestHandler):
    def get(self):
        upcCode = self.request.get('upc')
        if upcCode:
            errorCode = None
            #Support UPC-A codes by appending a 0 in front. EAN codes are 13
            #digits long so to look up the correct item we need to tweak the data here
            if len(upcCode) == 12:
                upcCode = '0' + upcCode;

            if len(upcCode) != 13:
                jsonResponse = simplejson.dumps({"success": False, "description": "UPC has invalid length. Expected 13 characters but got '%s'" % (upcCode), "error_code": INVALID_UPC_CODE})
            else:
                productDescription = ''
                result = {}
                #Check for entry in memcache and local DB. If not found, hit upcdatabase.com and look it up there
                localResult = self.lookupUpcDescriptionLocally(upcCode)
                if(localResult is not None):
                    result['description'] = localResult
                    result['found'] = True
                else:
                    upcDBResult = {}
                    rpcServer = xmlrpclib.ServerProxy(UPC_DATABASE_RPC_URL, GoogleXMLRPCTransport())
                    upcDBResult = rpcServer.lookupEAN(upcCode)
                    #If we've found a result, put the description found into our database and memcache
                    if type(upcDBResult) == dict and upcDBResult['found']:
                        upc = PBDatabase.UPC()
                        upc.code = upcCode
                        upc.description = upcDBResult['description']
                        upc.origin = 'upcdblookup'
                        upc.put()
                        memcache.set(upcCode, upcDBResult['description'])
                        result['description'] = upcDBResult['description']
                        result['found'] = True

                #self.response.out.write('%s = %r %s' % (result, result, type(result)))
                if type(result) == dict and result['found']:
                    productDescription = result['description']
                    links  = PBUtils.BossSearch().getResults(result['description'])
                    if len(links) == 0:
                        links  = PBUtils.GoogleSearch().getResults(result['description'])

                    if len(links) > 0:
                        beer_info = PBUtils.Scraper().doScrape(links[0])
                    else:
                        errorCode = NO_BEER_FOUND
                else:
                    errorCode = NO_UPC_FOUND

                if errorCode is None:
                    jsonResponse = simplejson.dumps({"success": True, "description": productDescription, "links": links, "beer_info":beer_info})
                else:
                    jsonResponse = simplejson.dumps({"success": False, "errorResponse": result, "error_code": errorCode})
        else:
            jsonResponse = simplejson.dumps({"success": False, "errorResponse": "No UPC Code Recieved", "error_code": NO_UPC_CODE_SENT})

        self.response.headers['Content-Length'] = len(jsonResponse)
        self.response.out.write(jsonResponse)

    def lookupUpcDescriptionLocally(self, upcCode):
        upcDescription = memcache.get(upcCode)
        if upcDescription is not None:
            return upcDescription
        else:
            datastoreEntry = db.GqlQuery("SELECT * FROM UPC WHERE code = :1", upcCode).fetch(1,0)
            if(len(datastoreEntry) > 0):
                #If we've found an entry in the DB, stick it into memcache
                if(datastoreEntry[0].description is not None and datastoreEntry[0].description != ""):
                    memcache.set(upcCode, datastoreEntry[0].description)
                    return datastoreEntry[0].description
        return None

def main():
    application = webapp.WSGIApplication([('/upclookup', MainHandler)], debug=True)
    run_wsgi_app(application)


if __name__ == '__main__':
    main()
