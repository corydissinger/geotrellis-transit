package geotrellis.transit.services.enterreturn

import geotrellis.transit.services._

import geotrellis.transit._
import geotrellis.rest._

import javax.ws.rs._
import javax.ws.rs.core.Response

import com.wordnik.swagger.annotations._

@Path("/categories")
@Api(value = "/categories", 
     description = "Query categories for resources.")
class TransitModesService extends ServiceUtil {
  @GET
  @Produces(Array("application/json"))
  @ApiOperation(
    value = "Returns the categories for a resource." , 
    notes = """

""")
  def get():Response = {
    val roots = Main.enterReturn.categories.map(_.root).toSet

    val categories = 
      (for(root <- roots) yield {
        val subcategories = {
          var subs = 
            Main.enterReturn.categories.filter(_.root == root)
          subs.map { s => s""""$s"""" }.mkString(",")
        }

         s"""
             {
              "name" : ${root},
              "subcategories" : [ $subcategories ]
             }
       """

      }).toSeq.mkString(",")

   val json =
    s"""
       {
        "categories": [ $categories ]
       }
     """
    OK.json(json).allowCORS
  }
}
