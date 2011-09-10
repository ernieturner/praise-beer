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
BOSS_SEARCH_API_KEY   = 'iwKJ.8nV34GERUNY2z2d3JeFQU8MzQi5vBdoAjzzcCGs175VukcNx1sfKvOlN529tImhNgZrrUE-'
BA_BEER_PROFILE_URL  = 'beeradvocate.com/beer/profile/'
# ----------------------------------------------------------------------------------------
# Scraper - This will scrape the BA site for us, or Google's cache if BA is being too slow
# ----------------------------------------------------------------------------------------
class Scraper():
  def doScrape(self,url):
    try:
      uniqueBeerCode = url.partition(BA_BEER_PROFILE_URL)
      uniqueBeerCode = uniqueBeerCode[2]
      if uniqueBeerCode != '':
        cachedData = memcache.get(uniqueBeerCode)
        if cachedData is not None and len(cachedData) > 0:
          return cachedData
      
      result = urlfetch.fetch(url,deadline=10)
      if result.status_code == 200:
        beerDetails = self.parseResults(result.content)
        if uniqueBeerCode != '' and len(beerDetails) > 0 and beerDetails['ratings'] and beerDetails['stats']:
            #Set memcache to expire in 10 days
            memcache.set(uniqueBeerCode, beerDetails, 864000)
        return beerDetails
      else:
        return "URL not found"
    except:
      try:
        cache_url = 'http://webcache.googleusercontent.com/search?q=cache:'+ url + '&hl=en'
        result = urlfetch.fetch(cache_url,deadline=10)
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

    ratingRegex = re.compile('BAscore_big">([A-Z/]+[/]?[+-]?)</span>\s*<br>\s*([a-z\s]+)<br>([0-9,]+) Reviews</td>.*THE BROS\s*<br/?><span\s+class="BAscore_big">([A-Z/]+[+-]?)</span>\s*<br/?>([a-z\s!;]+)')
    matches = ratingRegex.search(content)
    if matches:
      beer_info['ratings'] = {'overall': matches.group(1,2,3), 'bros': matches.group(4,5)}
      
      styleRegex = re.compile('<b>Style\s*[|]\s*ABV</b>\s*<br/?>\s*<a href="/beer/style/(\d+)"><b>([^<]*)</b></a>\s*[|]\s*&nbsp;\s*(?:(\d+\.?\d*?)%)')
      matches = styleRegex.search(content)
      if matches:
        beer_info['stats'] = {'abv': matches.group(3), 'style_name': matches.group(2), 'style_id': matches.group(1)}

        nameRegex = re.compile('<h1 class="norm">([^<]*)<')
        matches = nameRegex.search(content)
        if matches:
          beer_info['stats']['name'] = matches.group(1).strip()

      breweryRegex = re.compile('<b>Brewed by:</b>\s*<br/?>\s*<a href="/beer/profile/(\d+)">\s*<b>\s*([^<]+)')
      matches = breweryRegex.search(content)
      if matches:
         beer_info['stats']['brewery'] = matches.group(1,2)
      
    return beer_info
# -----------------------------------------------------------------------------
# BOSS API Search Handler Class
# -----------------------------------------------------------------------------

class BossSearch():
  def getResults(self, description, returnTitle=False):
    return []
    base   = 'http://boss.yahooapis.com/ysearch/web/v1/'
    params = {
      'appid':BOSS_SEARCH_API_KEY,
      'sites':'beeradvocate.com'
    }
    payload = urllib.urlencode(params)
    url     = base + urllib.quote('inurl:"beer/profile" ') + urllib.quote(description) + '?' + payload    
    response = StringIO(urlfetch.fetch(url).content)
    result   = self._formatResults(simplejson.load(response), returnTitle)
    return result

  def _formatResults(self, searchResults, returnTitle=False):    
    baBase = 'http://www.beeradvocate.com/beer/profile/'
    lookup = {}
    links  = []
        
    if int(searchResults['ysearchresponse']['count']) > 0:
      for entry in searchResults['ysearchresponse']['resultset_web']:
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
      return links
    return []

  def _removeHtmlTags(self, data):
    p = re.compile(r'<.*?>')
    return p.sub('', data)
  
# -----------------------------------------------------------------------------
# Google API Search Handler Class
# -----------------------------------------------------------------------------

class GoogleSearch():
  def getResults(self, description, returnTitle=False): 
    base   = "http://ajax.googleapis.com/ajax/services/search/web?"
    params = {
      'v':'1.0',
      #'key':GOOGLE_SEARCH_API_KEY,
      'q':'site:beeradvocate.com/beer/profile/ '+ description
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
