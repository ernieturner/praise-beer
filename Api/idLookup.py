#!/usr/bin/env python

# -----------------------------------------------------------------------------
# Imports
# -----------------------------------------------------------------------------
import wsgiref.handlers
import sys
import PBUtils
import re

from google.appengine.ext import webapp
from google.appengine.ext.webapp.util import run_wsgi_app
from django.utils import simplejson


# -----------------------------------------------------------------------------
# Constants
# -----------------------------------------------------------------------------

NO_ID_SENT = 1
FAILED_GETTING_RESULTS = 2

# -----------------------------------------------------------------------------
# Web Request Handler Class
# -----------------------------------------------------------------------------

class MainHandler(webapp.RequestHandler):
    def get(self):
        beerID = self.request.get('id')

        #Make sure a description was passed in
        if not beerID:
            self.printResponse(simplejson.dumps({"success": False, "error_code": NO_ID_SENT}))
            return

        beerInfo = PBUtils.Scraper().doScrape(PBUtils.BA_BEER_PROFILE_URL + beerID)

        if(not beerInfo or isinstance(beerInfo, str)):
            jsonResponse = simplejson.dumps({"success": False, "error": FAILED_GETTING_RESULTS})
        else:
            jsonResponse = simplejson.dumps({"success": True, "beer_info": beerInfo})

        self.printResponse(jsonResponse)

    def printResponse(self, response):
        self.response.headers['Content-Length'] = len(response)
        self.response.out.write(response)

def main():
    application = webapp.WSGIApplication([('/idlookup', MainHandler)], debug=True)
    run_wsgi_app(application)

if __name__ == '__main__':
    main()
