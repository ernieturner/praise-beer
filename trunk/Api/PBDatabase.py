#!/usr/bin/env python
from google.appengine.ext import db

class UPC(db.Model):
    code        = db.StringProperty(required=True)
    description = db.StringProperty()
    origin      = db.StringProperty()
    ba_link     = db.StringProperty()
    date        = db.DateTimeProperty(auto_now_add=True)

class ModificationHistory(db.Model):
    code = db.StringProperty(required=True)
    oldDescription = db.StringProperty()
    newDescription = db.StringProperty()
    modificationDate = db.DateTimeProperty(auto_now_add=True)
    verified = db.BooleanProperty(default=False)
