package geotrellis.transit

import geotrellis.transit.loader.Loader
import geotrellis.transit.loader.GraphFileSet
import geotrellis.transit.loader.gtfs.GtfsFiles
import geotrellis.transit.loader.osm.OsmFileSet
import geotrellis.network._
import geotrellis.network.graph._
import geotrellis.feature.SpatialIndex

import scala.collection.mutable

import geotrellis.rest.WebRunner

import java.io._

import com.wordnik.swagger.jaxrs.JaxrsApiReader

object Main {
  // Make swagger not do weird naming on API docs.
  JaxrsApiReader.setFormatString("")

  private var _context:GraphContext = null
  def context = _context

  def initContext(configPath:String) = {
    _context = Configuration.loadPath(configPath).graph.getContext
    println("Initializing shortest path tree array...")
    ShortestPathTree.initSptArray(context.graph.vertexCount)
  }

  def main(args:Array[String]):Unit = {
    if(args.length < 1) {
      Logger.error("Must use subcommand")
      System.exit(1)
    }

    def inContext(f:()=>Unit) = {
      val configPath = args(1)
      initContext(configPath)
      f
    }

    val call = 
      args(0) match {
        case "buildgraph" =>
          val configPath = args(1)
          () => buildGraph(configPath)
        case "server" =>
          inContext(() => mainServer(args))
        case s =>
          Logger.error(s"Unknown subcommand $s")
          System.exit(1)
          () => { }
      }

    call()
  }

  def buildGraph(configPath:String) = {
    Logger.log(s"Building graph data from configuration $configPath")
    val config = Configuration.loadPath(configPath)
    Loader.buildGraph(config.graph,config.loader.fileSets)
  }

  def mainServer(args:Array[String]) = {
    WebRunner.run { server =>
      server.context.addFilter(classOf[geotrellis.transit.services.ApiOriginFilter],
                               "/*",
                               java.util.EnumSet.noneOf(classOf[javax.servlet.DispatcherType]))
    }
  }
}
