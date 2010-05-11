#!/usr/bin/env python

# -----------------------------------------------------------------------------
# Imports
# -----------------------------------------------------------------------------
import wsgiref.handlers
import sys
import PBUtils
import PBDatabase

from google.appengine.ext import webapp
from google.appengine.ext.webapp.util import run_wsgi_app
from django.utils import simplejson
from google.appengine.ext import db
from google.appengine.api import memcache

# -----------------------------------------------------------------------------
# Constants
# -----------------------------------------------------------------------------

NO_UPC_CODE_SENT = 1
NO_BEER_FOUND = 3
RATING_LOOKUP_TIMEOUT = 4
NAME_LOOKUP_NO_DESCRIPTION_PROVIDED = 5

# -----------------------------------------------------------------------------
# Web Request Handler Class
# -----------------------------------------------------------------------------

class MainHandler(webapp.RequestHandler):
    def get(self):
        upcCode = self.request.get('upc')
        description = self.request.get('description')
        response = ""
        beerInfo = {}

        #Make sure a description was passed in
        if not description:
            self.response.out.write(simplejson.dumps({"success": False, "error_code": NO_DESCRIPTION_PROVIDED}))
            return

        #Make sure a UPC was passed in
        if not upcCode:
            self.response.out.write(simplejson.dumps({"success": False, "error_code": NO_UPC_CODE_SENT}))
            return

        #Find results for description entered
        links  = PBUtils.BossSearch().getResults(description)
        if len(links) == 0:
            links = PBUtils.GoogleSearch().getResults(description)

        #If no results were found, report error
        if len(links) > 0:
            beerInfo = PBUtils.Scraper().doScrape(links[0])
        else:
            self.response.out.write(simplejson.dumps({"success": False, "error_code": NO_BEER_FOUND}))
            return

        #Check if we already have an entry for this UPC code, if we don't, add it
        #datastoreEntry = db.GqlQuery("SELECT * FROM UPC WHERE code = :1", upcCode).fetch(1,0)
        #if(len(datastoreEntry) == 0):
        #    upcEntry             = PBDatabase.UPC()
        #    upcEntry.code        = upcCode
        #    upcEntry.description = description
        #    upcEntry.origin      = 'manualentry'
        #    upcEntry.put()

        jsonResponse = simplejson.dumps({"success": True, "description": description, "links": links, "beer_info":beerInfo})
        self.response.headers['Content-Length'] = len(jsonResponse)
        self.response.out.write(jsonResponse)

def main():
    application = webapp.WSGIApplication([('/namelookup', MainHandler)], debug=True)
    run_wsgi_app(application)


if __name__ == '__main__':
    main()
