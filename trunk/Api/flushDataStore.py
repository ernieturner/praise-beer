import PBDatabase

from google.appengine.ext import webapp
from google.appengine.ext.webapp.util import run_wsgi_app
from google.appengine.ext import db

class MainHandler(webapp.RequestHandler):
    def get(self):                
      q = db.GqlQuery("SELECT * FROM UPC WHERE origin = 'import'")
      results = q.fetch(100)
      for result in results:        
        result.delete()
    
def main():
    application = webapp.WSGIApplication([('/flushDataStore', MainHandler)], debug=True)
    run_wsgi_app(application)

if __name__ == '__main__':
    main()
