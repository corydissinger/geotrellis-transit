package geotrellis.transit

import geotrellis.transit.loader.gtfs.Csv
import java.net.URL
import geotrellis.feature._

import scala.collection.mutable

case class Category(root:String,name:String,url:URL) {
  def toJson() = {
    s"""{ "category" : "$root", "subcategory" : "$name", "url" : "${url.toString}" }"""
  }
}

case class Resource(category:Category,
                    name:String,
                    websites:List[String],
                    phones:List[String],
                    address:String,
                    zipCode:String,
                    emails:List[String],
                    description:String,
                    lat:Double,
                    lng:Double) {
  def toJson() = {
    val w = websites.map(x => s""""$x"""").mkString(",")
    val p = phones.map(x => s""""$x"""").mkString(",")
    val e = emails.map(x => s""""$x"""").mkString(",")

    s"""
     {
       "category" : ${category.toJson},
       "name" : "$name",
       "websites" : [ $w  ],
       "phones" : [ $p ],
       "address" : "$address",
       "zipCode" : "$zipCode",
       "emails"  : [ $e ],
       "description" : "$description",
       "lat" : $lat,
       "lng" : $lng
     }
    """
  }
}

case class EnterReturnData(
  categories:List[Category],
  index:SpatialIndex[Resource]
)

object EnterReturn {
  def readData():EnterReturnData = {
    val categories = {
      (for( entry <- Csv.fromPath("data/networkofcare_categories.csv")) yield {
        Category(entry("category"),entry("subcategory"),new URL(entry("subcategory_url")))
      }).toList
    }

    val resources =
      (for( entry <- Csv.fromPath("data/networkofcare_lat_lng.csv")) yield {
        Resource(
          Category(entry("category"),entry("subcategory"),new URL(entry("subcategory_url"))),
          entry("name"),
          entry("websites").split(",").toList,
          entry("phones").split(",").toList,
          entry("address"),
          entry("zipcode"),
          entry("emails").split(",").toList,
          entry("description"),
          entry("lat").toDouble,
          entry("lng").toDouble
        )
      }).toList

    val index = SpatialIndex(resources) { r => (r.lat,r.lng) }
    EnterReturnData(categories,index)
  }
}
