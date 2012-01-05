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
from google.appengine.api import memcache

# -----------------------------------------------------------------------------
# Constants
# -----------------------------------------------------------------------------

GOOGLE_SEARCH_API_KEY = 'ABQIAAAAAlIdqGCJUyFNZYYITSwQaxQMMZlHq7uMtE8oKCK3ertxke9vYhTldmsx1t8SNWeqeFA1Cqo-hQcWhw'
BA_BEER_PROFILE_URL  = 'beeradvocate.com/beer/profile/'
# ----------------------------------------------------------------------------------------
# Scraper - This will scrape the BA site for us, or Google's cache if BA is being too slow
# ----------------------------------------------------------------------------------------
class Scraper():
  def doScrape(self,url):
    uniqueBeerCode = url.partition(BA_BEER_PROFILE_URL)
    uniqueBeerCode = uniqueBeerCode[2]
    if uniqueBeerCode != '':
      cachedData = memcache.get(uniqueBeerCode)
      if cachedData is not None and len(cachedData) > 0:
        return cachedData
    
    try:  
      result = urlfetch.fetch(url, deadline=10)
      if result.status_code == 200:
        try:
          beerDetails = self.parseResults(result.content)
          if uniqueBeerCode != '' and len(beerDetails) > 0 and beerDetails['ratings'] and beerDetails['stats']:
              #Set memcache to expire in 10 days
              memcache.set(uniqueBeerCode, beerDetails, 864000)
          return beerDetails
        except:
          return "Error with regex matching or memcache setting"
      else:
        return "URL not found"
    except:
      try:
        cache_url = 'http://webcache.googleusercontent.com/search?q=cache:'+ url + '&hl=en'
        result = urlfetch.fetch(cache_url, deadline=10)
        if result.status_code == 200:
          beerDetails = self.parseResults(result.content)
          if uniqueBeerCode != '' and len(beerDetails) > 0 and beerDetails['ratings'] and beerDetails['stats']:
             #Set memcache to expire in 10 days
            memcache.set(uniqueBeerCode, beerDetails, 864000)
          return beerDetails
        else:
          return 'An error occurred during URL fetch ('+ result.status.code +').'
      except:
        return "Request timed out"
    return "No results found."


  def parseResults(self, content):
    content = re.sub('\n|\r', '', content)
    beer_info = {}
    ratingRegex = re.compile('BAscore_big">([0-9]+)</span>.*<b>([a-z]+).*<br>-<br>([0-9,]+)\s*Reviews.*BAscore_big">([0-9NA/]+)</span>.*=ratings"><b>([a-z]*)</b>')
    matches = ratingRegex.search(content)
    if matches:
      beer_info['ratings'] = {'overall': matches.group(1,2,3), 'bros': matches.group(4,5)}
      styleRegex = re.compile('<b>Style\s*[|]\s*ABV.*/beer/style/(\d+)"><b>([^<]*)</b></a>\s*[|]\s*&nbsp;\s*(?:(\d+\.?\d*?)%)')
      matches = styleRegex.search(content)
      if matches:
        beer_info['stats'] = {'abv': matches.group(3), 'style_name': matches.group(2), 'style_id': matches.group(1)}

        nameRegex = re.compile('<h1 class="norm">([^<]*)<')
        matches = nameRegex.search(content)
        if matches:
          beer_info['stats']['name'] = matches.group(1).strip()

      breweryRegex = re.compile('<b>Brewed by:.*/beer/profile/(\d+)">\s*<b>\s*([^<]+)')
      matches = breweryRegex.search(content)
      if matches:
         beer_info['stats']['brewery'] = matches.group(1,2)
      
    return beer_info

  
# -----------------------------------------------------------------------------
# Google API Search Handler Class
# -----------------------------------------------------------------------------

class GoogleSearch():
  def getResults(self, description, returnTitle=False): 
    base   = "http://ajax.googleapis.com/ajax/services/search/web?"
    params = {
      'v':'1.0',
      'q':'site:' + BA_BEER_PROFILE_URL + ' ' + description
    }
    payload = urllib.urlencode(params)
    url     = base + payload

    response = StringIO(urlfetch.fetch(url).content)    
    result   = self._formatResults(simplejson.load(response), returnTitle)
    return result;          

  def _formatResults(self, searchResults, returnTitle=False):        
    baBase = 'http://www.beeradvocate.com/beer/profile/'    
    lookup = {}
    links  = []
    for entry in searchResults['responseData']['results']:
      if re.search(r"\/(\d+)\/(\d+)\/?",entry['url']):
        m = re.search(r"\/(\d+)\/(\d+)\/?",entry['url'])
        key = baBase + m.group(1) + '/'+ m.group(2)
        title = self._removeHtmlTags(entry['title'])
        if not lookup.has_key(key):
          lookup[key] = 1
          if returnTitle == True:
            links.append({'url':m.group(1) + '/' + m.group(2), 'title':title})
          else:
            links.append(key)
          links.append(key)
    return links
  
  def _removeHtmlTags(self, data):
      p = re.compile(r'<.*?>')
      return p.sub('', data)
