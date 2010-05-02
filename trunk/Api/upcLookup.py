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

from google.appengine.api import urlfetch
from google.appengine.ext import webapp
from google.appengine.ext.webapp.util import run_wsgi_app
from django.utils import simplejson


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
            #Support UPC-A codes by appending a 0 in front. EAN codes are 13
            #digits long so to look up the correct item we need to tweak the data here
            if len(upcCode) == 12:
                upcCode = '0' + upcCode;

            if len(upcCode) != 13:
                jsonResponse = simplejson.dumps({"success": False, "description": "UPC has invalid length. Expected 13 characters but got '%s'" % (upcCode), "error_code": INVALID_UPC_CODE})
            else:
                productDescription = ''
                error = False
                
                rpcServer = xmlrpclib.ServerProxy(UPC_DATABASE_RPC_URL, GoogleXMLRPCTransport())
                result = rpcServer.lookupEAN(upcCode)
                #self.response.out.write('%s = %r %s' % (result, result, type(result)))
                if type(result) == dict and result['found']:
                    productDescription = result['description']                    
                    links     = PBUtils.GoogleSearch().getResults(result['description'])
                    beer_info = PBUtils.Scraper().doScrape(links[0])
                    #size  = result['size']
                else:
                    error = True

                if error == False:
                    jsonResponse = simplejson.dumps({"success": True, "description": productDescription, "links": links, "beer_info":beer_info})
                else:
                    jsonResponse = simplejson.dumps({"success": False, "errorResponse": result, "error_code": NO_UPC_FOUND})
        else:
            jsonResponse = simplejson.dumps({"success": False, "errorResponse": "No UPC Code Recieved", "error_code": NO_UPC_CODE_SENT})

        #This probably isn't really neccessary anyway. It also makes it tough to debug in a browser
        #self.response.headers['Content-Type'] = 'text/json'
        self.response.headers['Content-Length'] = len(jsonResponse)
        self.response.out.write(jsonResponse + "<br><br>")               

def main():
    application = webapp.WSGIApplication([('/upclookup', MainHandler)], debug=True)
    run_wsgi_app(application)


if __name__ == '__main__':
    main()
