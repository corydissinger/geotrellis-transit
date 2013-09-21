package geotrellis.transit

import geotrellis.transit.loader.gtfs.Csv
import java.net.URL
import geotrellis.feature._

import scala.collection.mutable

case class Category(root:String,name:String,url:URL)

case class Resource(category:Category,
                    name:String,
                    websites:List[String],
                    phones:List[String],
                    addresses:List[String],
                    zipCodes:List[String],
                    emails:List[String],
                    description:String,
                    lat:Double,
                    lng:Double)

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

    val resources = {
      val rs = mutable.Map[(String,String),Resource]()

      for( entry <- Csv.fromPath("data/networkofcare.csv")) {
        val name = entry("name")
        val phones = entry("phones")
        if(!rs.contains((name,phones))) {
          rs((name,phones)) =
            Resource(
              Category(entry("category"),entry("subcategory"),new URL(entry("subcategory_url"))),
              entry("name"),
              entry("websites").split(",").toList,
              entry("phones").split(",").toList,
              List(entry("address")),
              List(entry("zipcode")),
              entry("emails").split(",").toList,
              entry("description"),
              entry("lat").toDouble,
              entry("lng").toDouble
            )
        } else {
          val r = rs((name,phones))
          rs((name,phones)) =
            Resource(r.category,
                     r.name,
                     r.websites,
                     r.phones,
                     entry("address") :: r.addresses,
                     entry("zipcode") :: r.zipCodes,
                     r.emails,
                     r.description,
                     r.lat,
                     r.lng)
        }
      }
      rs.values.toList
    }

    val index = SpatialIndex(resources) { r => (r.lat,r.lng) }
    EnterReturnData(categories,index)
  }
}
