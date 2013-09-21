package geotrellis.transit.services.enterreturn

import javax.ws.rs._

import com.wordnik.swagger.annotations._

@Path("/enterreturn")
@Api(value = "/enterreturn", 
   description = "Enter Return endpoints.")
class TravelShedService extends VectorResource 
                           // with WmsResource
                           // with ExportResource
