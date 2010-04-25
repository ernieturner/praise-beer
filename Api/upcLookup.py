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


from google.appengine.api import urlfetch
from google.appengine.ext import webapp
from google.appengine.ext.webapp.util import run_wsgi_app
from django.utils import simplejson

# -----------------------------------------------------------------------------
# Constants
# -----------------------------------------------------------------------------

UPC_DATABASE_RPC_URL = 'http://www.upcdatabase.com/rpc'
GOOGLE_SEARCH_API_KEY = 'ABQIAAAAAlIdqGCJUyFNZYYITSwQaxQMMZlHq7uMtE8oKCK3ertxke9vYhTldmsx1t8SNWeqeFA1Cqo-hQcWhw'

# -----------------------------------------------------------------------------
# Custom RPC Transport Class
# -----------------------------------------------------------------------------

class GoogleXMLRPCTransport(object):
    """Handles an HTTP transaction to an XML-RPC server."""

    def __init__(self, use_datetime=0):
        self._use_datetime = use_datetime

    def request(self, host, handler, request_body, verbose=0):
        """
        Send a complete request, and parse the response. See xmlrpclib.py.

        :Parameters:
            host : str
                target host
                
            handler : str
                RPC handler on server (i.e., path to handler)
                
            request_body : str
                XML-RPC request body
                
            verbose : bool/int
                debugging flag. Ignored by this implementation

        """

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
# Google API Search Handler Class
# -----------------------------------------------------------------------------

class GoogleSearch():
	def getResults(self,description):		
		base   = "http://ajax.googleapis.com/ajax/services/search/web?"		
		params = {
			'v':'1.0',
			'key':GOOGLE_SEARCH_API_KEY,
			'q':'site:beeradvocate.com '+ description
		}
		payload = urllib.urlencode(params)
		url     = base + payload
					
		response = StringIO(urlfetch.fetch(url).content)		
		result   = self._formatResults(simplejson.load(response))
		return result;					

	def _formatResults(self,searchResults):        
		baBase = 'http://www.beeradvocate.com/beer/profile/'		
		lookup = {};
		for entry in searchResults['responseData']['results']:
			if re.search(r"\/(\d+)\/(\d+)\/?",entry['url']):
				m = re.search(r"\/(\d+)\/(\d+)\/?",entry['url'])														
				lookup[baBase + m.group(1) + '/'+ m.group(2)] = 1;
			else:									
				lookup[baBase + m.group(1) + '/'+ m.group(2)] = 1;
		return lookup.keys();
					
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
                jsonResponse = simplejson.dumps({"success": False, "description": "UPC has invalid length. Expected 13 characters but got '%s'" % (upcCode)})
            else:
                productDescription = ''
                error = False
                
                rpcServer = xmlrpclib.ServerProxy(UPC_DATABASE_RPC_URL, GoogleXMLRPCTransport())
                result = rpcServer.lookupEAN(upcCode)
                #self.response.out.write('%s = %r %s' % (result, result, type(result)))
                if type(result) == dict and result['found']:
                    productDescription = result['description']                    
                    links = GoogleSearch().getResults(result['description'])
                    #size = result['size']
                else:
                    error = True

                if error == False:
                    jsonResponse = simplejson.dumps({"success": True, "description": productDescription, "links": links})
                else:
                    jsonResponse = simplejson.dumps({"success": False, "errorResponse": result})
        else:
            jsonResponse = simplejson.dumps({"success": False, "errorResponse": "No UPC Code Recieved"})

        #This probably isn't really neccessary anyway. It also makes it tough to debug in a browser
        #self.response.headers['Content-Type'] = 'text/json'
        self.response.headers['Content-Length'] = len(jsonResponse)
        self.response.out.write(jsonResponse + "<br><br>")               

def main():
    application = webapp.WSGIApplication([('/upclookup', MainHandler)], debug=True)
    run_wsgi_app(application)


if __name__ == '__main__':
    main()
