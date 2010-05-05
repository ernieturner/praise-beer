from google.appengine.ext import webapp
from google.appengine.ext.webapp.util import run_wsgi_app
from google.appengine.ext import db

class UPC(db.Model):
    code        = db.StringProperty()
    description = db.StringProperty()
    origin      = db.StringProperty()
    date        = db.DateTimeProperty(auto_now_add=True)
    
class MainHandler(webapp.RequestHandler):
    def get(self):				
				# Open our file of known UPCs and descriptions for import
				f = open('./known_upc_data.txt', 'r')
				
				for line in f:
					line = line.rstrip("\n")					
					upc = UPC()
					[upc.code, upc.description] = line.split("\t")
					upc.origin = 'import'        
					upc.put()					
				f.close()

def main():
    application = webapp.WSGIApplication([('/populateUPCs', MainHandler)], debug=True)
    run_wsgi_app(application)


if __name__ == '__main__':
    main()