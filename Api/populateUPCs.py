import PBDatabase

from google.appengine.ext import webapp
from google.appengine.ext.webapp.util import run_wsgi_app
from google.appengine.api import memcache

class MainHandler(webapp.RequestHandler):
    def get(self):                
        # flush the memcache
        memcache.flush_all()
				
        # Open our file of known UPCs and descriptions for import
        f = open('./known_upc_data.txt', 'r')

        for line in f:
            line = line.rstrip("\n")
            line = line.rstrip("\r")
            upc = PBDatabase.UPC()
            [upc.code, upc.description] = line.split("\t")
            upc.origin = 'import'
            upc.put()	
        f.close()

def main():
    application = webapp.WSGIApplication([('/populateUPCs', MainHandler)], debug=True)
    run_wsgi_app(application)

if __name__ == '__main__':
    main()
