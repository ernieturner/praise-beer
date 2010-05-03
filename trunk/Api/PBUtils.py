#!/usr/bin/env python

# -----------------------------------------------------------------------------
# Imports
# -----------------------------------------------------------------------------
import sys
import logging
from StringIO import StringIO
import urllib
import re

from google.appengine.api import urlfetch
from google.appengine.api.urlfetch import DownloadError 
from django.utils import simplejson

# -----------------------------------------------------------------------------
# Constants
# -----------------------------------------------------------------------------

GOOGLE_SEARCH_API_KEY = 'ABQIAAAAAlIdqGCJUyFNZYYITSwQaxQMMZlHq7uMtE8oKCK3ertxke9vYhTldmsx1t8SNWeqeFA1Cqo-hQcWhw'
BOSS_SEARCH_API_KEY = 'iwKJ.8nV34GERUNY2z2d3JeFQU8MzQi5vBdoAjzzcCGs175VukcNx1sfKvOlN529tImhNgZrrUE-'
# -----------------------------------------------------------------------------
# Scraper - This will scrape the BA site for us, or Google's cache, depending
# -----------------------------------------------------------------------------
class Scraper():

	def doScrape(self,url):
		try: 				
			result = urlfetch.fetch(url,deadline=10)
			if result.status_code == 200:		  	
				content = re.sub('\n|\r','',result.content)
				pat = re.compile('BAscore_big">([A-Z/]+[/]?[+-]?)</span><br>([a-z\s]+)<br>w/ ([0-9,]+) Reviews</td>.*THE BROS<br><span class="BAscore_big">([A-Z/]+[+-]?)</span><br>([a-z\s]+)')
				if pat.search(content):
					m = pat.search(content)
					beer_info = {'ratings':{'overall':m.group(1,2,3), 'bros':m.group(4,5)}}
					
					pat = re.compile('<b>Style \| ABV</b><br><a href="/beer/style/(\d+)"><b>([a-zA-Z\s()]+)</b></a> \| &nbsp;(\d+\.?\d+)% <a')
					if pat.search(content):
						m = pat.search(content)
						beer_info['stats'] = {'abv':m.group(3), 'style_name':m.group(2), 'style_id':m.group(1)}
										
					return beer_info
		except:
			try:														  
				cache_url = 'http://webcache.googleusercontent.com/search?q=cache:'+ url + '&hl=en&strip=1'
				result = urlfetch.fetch(cache_url,deadline=10)
				if result.status_code == 200:		  		
					content = re.sub('\n|\r','',result.content)
					pat = re.compile('BA OVERALL<br>([A-Z/]+[+-]?)<br>([a-z\s]+)<br>w/ ([0-9,]+) Reviews</td>.*THE BROS<br>([A-Z/]+[+-]?)<br>([a-z\s!;]+)')
					if pat.search(content):						
						m = pat.search(content)						
						beer_info = {'ratings':{'overall':m.group(1,2,3), 'bros':m.group(4,5)}}

						pat = re.compile('<b>Style \| ABV</b><br><a href="/beer/style/(\d+)"><b>([a-zA-Z\s()]+)</b></a> \| &nbsp;(\d+\.?\d+)% <a')
						if pat.search(content):
							m = pat.search(content)
							beer_info['stats'] = {'abv':m.group(3), 'style_name':m.group(2), 'style_id':m.group(1)}
						
						return beer_info
					else:
						return 'No matches found.'
				else:
					return 'An error occurred during URL fetch ('+ result.status.code +').'		
			except:
				return "Request timed out"	
				
		return "No results found."
	
# -----------------------------------------------------------------------------
# BOSS API Search Handler Class
# -----------------------------------------------------------------------------

class BossSearch():
	def getResults(self,description):
		base   = "http://boss.yahooapis.com/ysearch/web/v1/"
		params = {
			'appid':BOSS_SEARCH_API_KEY,
			'sites':'beeradvocate.com/beer/profile'
		}
		payload = urllib.urlencode(params)
		url     = base + urllib.quote(description) + '?' + payload

		response = StringIO(urlfetch.fetch(url).content)		
		result   = self._formatResults(simplejson.load(response))
		return result;

	def _formatResults(self,searchResults):        
		baBase = 'http://www.beeradvocate.com/beer/profile/'
		lookup = {};
		for entry in searchResults['ysearchresponse']['resultset_web']:
			if re.search(r"\/(\d+)\/(\d+)\/?",entry['url']):
				m = re.search(r"\/(\d+)\/(\d+)\/?",entry['url'])														
				lookup[baBase + m.group(1) + '/'+ m.group(2)] = 1;
		return lookup.keys();

	
# -----------------------------------------------------------------------------
# Google API Search Handler Class
# -----------------------------------------------------------------------------

class GoogleSearch():
	def getResults(self,description):	
		base   = "http://ajax.googleapis.com/ajax/services/search/web?"
		params = {
			'v':'1.0',
			'key':GOOGLE_SEARCH_API_KEY,
			'q':'site:beeradvocate.com/beer/profile/ '+ description
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
