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

NO_SEARCH_TERM = 1
NO_RESULTS_FOUND = 2

# -----------------------------------------------------------------------------
# Web Request Handler Class
# -----------------------------------------------------------------------------

class MainHandler(webapp.RequestHandler):
    def get(self):
        searchTerm = self.request.get('keyword')
        searchResults = {}

        #Make sure a description was passed in
        if not searchTerm:
            self.printResponse(simplejson.dumps({"success": False, "error_code": NO_SEARCH_TERM}))
            return

        #Find results for description entered
        links  = PBUtils.BossSearch().getResults(searchTerm, True)

        #If no results were found, report error
        if len(links) == 0:
            self.printResponse(simplejson.dumps({"success": False, "error_code": NO_RESULTS_FOUND}))
            return

        self.printResponse(simplejson.dumps({"success" : True, "results" : links}))
        return

    def printResponse(self, response):
        self.response.headers['Content-Length'] = len(response)
        self.response.out.write(response)

def main():
    application = webapp.WSGIApplication([('/namesearch', MainHandler)], debug=True)
    run_wsgi_app(application)

if __name__ == '__main__':
    main()
