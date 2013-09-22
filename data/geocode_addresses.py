import csv
import StringIO
import urllib2
import json
import sys

def print_row(row):
	print >> sys.stdout, ','.join([repr(el) for el in row])

if __name__ == '__main__':
	headers = None
	address_field = None

	base_url = 'http://services.phila.gov/ULRS311/Data/Location/'

	new_data = []

	with open('networkofcare.csv') as f:
		reader = csv.reader(f)

		for row in reader:
			if headers is None:
				headers = row
				address_field = headers.index('address')
				continue

			try:
				addr = row[address_field].decode("utf-8", "ignore")

				safe_addr = ""
				for c in addr:
					if not (c == u'\xa0' or c == u'\xc2'):
						safe_addr += c

				encoded_addr = urllib2.quote(safe_addr)
				full_url = base_url + encoded_addr

				response = urllib2.urlopen(full_url)
				data = json.loads(response.read())
				lng = str(data['Locations'][0]['XCoord'])
				lat = str(data['Locations'][0]['YCoord'])
				print_row(row + [lat, lng])

			except Exception, e:
				print >> sys.stderr, e
