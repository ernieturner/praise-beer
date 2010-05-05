#!/usr/bin/env python
from google.appengine.ext import db

class UPC(db.Model):
    code        = db.StringProperty()
    description = db.StringProperty()
    origin      = db.StringProperty()
    date        = db.DateTimeProperty(auto_now_add=True)
