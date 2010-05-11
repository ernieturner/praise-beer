import PBDatabase

from google.appengine.ext import webapp
from google.appengine.ext.webapp.util import run_wsgi_app
from google.appengine.api import memcache

class MainHandler(webapp.RequestHandler):
    def get(self):
      upcCode = self.request.get('upc')
      if upcCode:
        memcache.delete(upcCode)
      else:
        memcache.flush_all()
    
def main():
    application = webapp.WSGIApplication([('/flushMemcache', MainHandler)], debug=True)
    run_wsgi_app(application)

if __name__ == '__main__':
    main()
