package geotrellis.transit.services.enterreturn

import javax.ws.rs._
import javax.ws.rs.core.Response
import com.wordnik.swagger.annotations._

import geotrellis.transit._
import geotrellis.transit.services._

import geotrellis._
import geotrellis.raster.op.ToVector
import geotrellis.rest._
import geotrellis.feature._
import geotrellis.feature.op._

import com.vividsolutions.jts.{geom => jts}

trait VectorResource extends ServiceUtil{
  @GET
  @Path("/json")
  @Produces(Array("application/json"))
  @ApiOperation(
    value = "Returns Resources based on a travelshed.", 
    notes = """

""")
  def getVector(
    @ApiParam(value = "Latitude of origin point", 
              required = true, 
              defaultValue = "39.957572")
    @DefaultValue("39.957572")
    @QueryParam("latitude") 
    latitude: Double,
    
    @ApiParam(value = "Longitude of origin point", 
              required = true, 
              defaultValue = "-75.161782")
    @DefaultValue("-75.161782")
    @QueryParam("longitude") 
    longitude: Double,
    
    @ApiParam(value = "Starting time of trip, in seconds from midnight", 
              required = true, 
              defaultValue = "0")
    @DefaultValue("0")
    @QueryParam("time") 
    time: Int,
    
    @ApiParam(value="Maximum duration of trip, in seconds", 
              required=true, 
              defaultValue="1800")
    @DefaultValue("1800")
    @QueryParam("duration")
    duration: Int,

    @ApiParam(value="""
Modes of transportation. Must be one of the modes returned from /transitmodes, case insensitive.
""",
              required=true, 
              defaultValue="walking")
    @DefaultValue("walking")
    @QueryParam("modes")  
    modes:String,

    @ApiParam(value="Schedule for public transportation. One of: weekday, saturday, sunday", 
              required=false, 
              defaultValue="weekday")
    @DefaultValue("weekday")
    @QueryParam("schedule")
    schedule:String,
 
    @ApiParam(value="Direction of travel. One of: departing,arriving", 
              required=true, 
              defaultValue="departing")
    @DefaultValue("departing")
    @QueryParam("direction")
    direction:String,

    @ApiParam(value="Number of columns for the traveshed raster to be vectorized.",
              required=true,
              defaultValue="200")
    @DefaultValue("200")
    @QueryParam("cols") 
    cols: Int,

    @ApiParam(value="Number of rows for the traveshed raster to be vectorized.",
              required=true,
              defaultValue="200")
    @DefaultValue("200")
    @QueryParam("rows") 
    rows: Int,

    @ApiParam(value="Categories. In comma seperated, 'root|subcategory,rootsubcategory' form",
              required=true,
              defaultValue="all")
    @DefaultValue("all")
    @QueryParam("categories") 
    categoriesString: String
  ): Response = {
    val categories:List[Category] =
      if(categoriesString == "all") {
        Main.enterReturn.categories
      } else {
        categoriesString
                  .split(",")
                  .map { pair =>
                     val cats = 
                       pair.split("|")
                     if(cats.length != 2) {
                       return ERROR("Categories must be comma separated list of category|subcategory.")
                     } else {
                       Main.enterReturn.categories.find { c => 
                         c.root == cats(0) && c.name == cats(1)
                       } match {
                         case Some(cat) => cat
                         case None => 
                           return ERROR(s"Unknown category ${cats(0)}|${cats(1)}")
                       }
                     }
                   }
                  .toList
      }

    val request = 
      try{
        SptInfoRequest.fromParams(
          latitude,
          longitude,
          time,
          duration,
          modes,
          schedule,
          direction)
      } catch {
        case e:Exception =>
          return ERROR(e.getMessage)
      }

    val sptInfo = SptInfoCache.get(request)

    val resourcesOp:Op[List[Resource]] =
      sptInfo.vertices match {
        case Some(ReachableVertices(subindex, extent)) =>
          val re = RasterExtent(expandByLDelta(extent), cols, rows)
          Literal(TravelTimeRaster(re, re, sptInfo,ldelta))
          // Set all relevant times to 1, everything else to NODATA
            .into(logic.RasterMapIfSet(_)(z => if(z <= duration) { 1 } else { NODATA }))
          // Vectorize
            .into(ToVector(_).map(_.toSeq))
          // Map the individual Vectors into one MultiPolygon
            .map { vectors =>
            val geoms =
              vectors.map(_.geom.asInstanceOf[jts.Polygon])

            val multiPolygonGeom =
              Feature.factory.createMultiPolygon(geoms.toArray)

            // Get bounding box
            val env = multiPolygonGeom.getEnvelopeInternal
            val extent = Extent(env.getMinX,env.getMinY,env.getMaxX,env.getMaxY)
            println(s"EXTENT: $extent")
            val pie = Main.enterReturn.index.pointsInExtent(extent)
            println(s"LENGHT OF RESOURCES IS ${pie.length}")
            println(s"LENGHT OF CATEGORIES IS ${categories.length}")
            val catps = 
              pie
                                  .filter { resource => categories.contains(resource.category) }

            println(s"CATEGORY RESOURCES IS ${catps.length}")
               catps
                                  .filter { resource =>
              val p =
                Feature.factory.createPoint(
                  new jts.Coordinate(resource.lng,resource.lat)
                )
              multiPolygonGeom.intersects(p)
                                   }
                                  .toList
          }
        case None => 
          println("UNREACHABLE!")
          Literal(List[Resource]())
      }

    val jsonOp = 
      resourcesOp.map { resources =>
        val jsons = resources.map(_.toJson).mkString(",")
        s"""
{
  "resources" : [ $jsons ]
}
"""
      }

    GeoTrellis.run(jsonOp) match {
      case process.Complete(json, h) =>
        OK.json(json)
      case process.Error(message, failure) =>
        ERROR(message, failure)
    }
  }
}
