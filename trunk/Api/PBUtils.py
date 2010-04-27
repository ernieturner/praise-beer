#!/usr/bin/env python

# -----------------------------------------------------------------------------
# Imports
# -----------------------------------------------------------------------------
import wsgiref.handlers
import sys
import logging
from StringIO import StringIO
import urllib
import re

from google.appengine.api import urlfetch
from google.appengine.api.urlfetch import DownloadError 
from google.appengine.ext import webapp
from google.appengine.ext.webapp.util import run_wsgi_app
from django.utils import simplejson

# -----------------------------------------------------------------------------
# Constants
# -----------------------------------------------------------------------------

GOOGLE_SEARCH_API_KEY = 'ABQIAAAAAlIdqGCJUyFNZYYITSwQaxQMMZlHq7uMtE8oKCK3ertxke9vYhTldmsx1t8SNWeqeFA1Cqo-hQcWhw'

# -----------------------------------------------------------------------------
# Scraper - This will scrape the BA site for us
# -----------------------------------------------------------------------------
class Scraper():

	def doScrape(self,url):
		try: 				
			result = urlfetch.fetch(url)
			if result.status_code == 200:		  	
				content = re.sub('\n|\r','',result.content)
				pat = re.compile('BAscore_big">([A-Z/]+[/]?[+-]?)</span><br>([a-z\s]+)<br>w/ ([0-9,]+) Reviews</td>.*THE BROS<br><span class="BAscore_big">([A-Z/]+[+-]?)</span><br>([a-z\s]+)')
				if pat.search(content):
					m = pat.search(content)
					ratings = {'overall':m.group(1,2,3), 'bros':m.group(4,5)}
					return ratings
		except:
			try:							  
				cache_url = 'http://webcache.googleusercontent.com/search?q=cache:'+ url + '&hl=en&strip=1'				
				result = urlfetch.fetch(cache_url)				
				if result.status_code == 200:		  		
					content = re.sub('\n|\r','',result.content)
					pat = re.compile('BA OVERALL<br>([A-Z/]+[+-]?)<br>([a-z\s]+)<br>w/ ([0-9,]+) Reviews</td>.*THE BROS<br>([A-Z/]+[+-]?)<br>([a-z\s!;]+)')
					if pat.search(content):
						m = pat.search(content)
						ratings = {'overall':m.group(1,2,3), 'bros':m.group(4,5)}
						return ratings
					else:
						return 'no matches'+content
				else:
					return 'nope'		
			except:
				return "Request Timed Out"	
				
		return "no results"
		
# -----------------------------------------------------------------------------
# Google API Search Handler Class
# -----------------------------------------------------------------------------

class GoogleSearch():
	def getResults(self,description):		
		base   = "http://ajax.googleapis.com/ajax/services/search/web?"		
		params = {
			'v':'1.0',
			'key':GOOGLE_SEARCH_API_KEY,
			'q':'site:beeradvocate.com '+ description
		}
		payload = urllib.urlencode(params)
		url     = base + payload
					
		response = StringIO(urlfetch.fetch(url).content)		
		result   = self._formatResults(simplejson.load(response))
		return result;					

	def _formatResults(self,searchResults):        
		baBase = 'http://www.beeradvocate.com/beer/profile/'		
		lookup = {};
		for entry in searchResults['responseData']['results']:
			if re.search(r"\/(\d+)\/(\d+)\/?",entry['url']):
				m = re.search(r"\/(\d+)\/(\d+)\/?",entry['url'])														
				lookup[baBase + m.group(1) + '/'+ m.group(2)] = 1;
			else:									
				lookup[baBase + m.group(1) + '/'+ m.group(2)] = 1;
		return lookup.keys();		