#!/usr/bin/env python

# -----------------------------------------------------------------------------
# Imports
# -----------------------------------------------------------------------------
import wsgiref.handlers
import sys
import PBUtils
import PBDatabase
import re

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
        modification = self.request.get('mod')
        response = ""
        beerInfo = {}

        #Make sure a description was passed in
        if not description:
            self.printResponse(simplejson.dumps({"success": False, "error_code": NO_DESCRIPTION_PROVIDED}))
            return

        #Make sure a UPC was passed in
        if not upcCode:
            self.printResponse(simplejson.dumps({"success": False, "error_code": NO_UPC_CODE_SENT}))
            return

        #Find results for description entered
        links  = PBUtils.BossSearch().getResults(description)
        if len(links) == 0:
            links = PBUtils.GoogleSearch().getResults(description)

        #If no results were found, report error
        if len(links) > 0:
            beerInfo = PBUtils.Scraper().doScrape(links[0])
        else:
            self.printResponse(simplejson.dumps({"success": False, "error_code": NO_BEER_FOUND}))
            return
        
        if type(beerInfo) != dict:
          self.printResponse(simplejson.dumps({"success": False, "error_code": NO_BEER_FOUND}))
          return
          
        #Check if we already have an entry for this UPC code, if we don't, add it. If this
        #is a modification request, then edit the DB and make a different flag
        datastoreEntry = db.GqlQuery("SELECT * FROM UPC WHERE code = :1", upcCode).get()
        if(datastoreEntry is None):
            baLink = ''
            if re.search(r"\/(\d+)\/(\d+)\/?", links[0]):
                m = re.search(r"\/(\d+)\/(\d+)\/?", links[0])
                baLink = m.group(1) + '/' + m.group(2)
            upcEntry = PBDatabase.UPC(code=upcCode,
                                      description=description,
                                      origin = 'manualentry:pending',
                                      ba_link = baLink)
            upcEntry.put()
        #We already have an entry, but we're processing a modification request
        elif(modification == '1'):
            entryModification = PBDatabase.ModificationHistory(code=upcCode,
                                                               oldDescription=datastoreEntry.description,
                                                               newDescription=description)
            entryModification.put()
            datastoreEntry.origin = 'manualentry:edit'
            datastoreEntry.description = description
            datastoreEntry.put()
        
        jsonResponse = simplejson.dumps({"success": True, "description": description, "links": links, "beer_info":beerInfo})
        self.printResponse(jsonResponse)

    def printResponse(self, response):
        self.response.headers['Content-Length'] = len(response)
        self.response.out.write(response)

def main():
    application = webapp.WSGIApplication([('/namelookup', MainHandler)], debug=True)
    run_wsgi_app(application)

if __name__ == '__main__':
    main()
