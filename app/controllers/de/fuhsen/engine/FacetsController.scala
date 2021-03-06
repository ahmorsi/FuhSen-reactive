package controllers.de.fuhsen.engine

import javax.inject.Inject

import controllers.de.fuhsen.FuhsenVocab
import org.apache.jena.query.{ResultSet, Syntax, QueryExecutionFactory, QueryFactory}
import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.apache.jena.riot.Lang
import play.Logger
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller}
import utils.dataintegration.RDFUtil
import play.api.libs.json._
/**
  * Created by dcollarana on 6/23/2016.
  */
class FacetsController @Inject()(ws: WSClient) extends Controller {

  def getFacets(uid: String, entityType: String) = Action { request =>
    Logger.info("Facets for search : " + uid + " entityType: "+entityType)

    val model = ModelFactory.createDefaultModel()

    entityType match {
      case "person" =>
        //Creating fs:Search resource
        model.createResource(FuhsenVocab.FACET_URI + "Gender")
          .addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "gender")
        model.createResource(FuhsenVocab.FACET_URI + "Birthday")
          .addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "birthday")
        model.createResource(FuhsenVocab.FACET_URI + "Location")
          .addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "location")
        model.createResource(FuhsenVocab.FACET_URI + "Occupation")
          .addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "occupation")
        model.createResource(FuhsenVocab.FACET_URI + "LiveIn")
          .addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "liveIn")
        model.createResource(FuhsenVocab.FACET_URI + "WorkAt")
          .addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "workAt")
        model.createResource(FuhsenVocab.FACET_URI + "StudyAt")
          .addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "studyAt")
      case "organization" =>
        //Creating fs:Search resource
        model.createResource(FuhsenVocab.FACET_URI + "Location")
          .addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "location")
        model.createResource(FuhsenVocab.FACET_URI + "Country")
          .addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "country")
      case "product" =>
        //Creating fs:Search resource
        model.createResource(FuhsenVocab.FACET_URI + "Price")
          .addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "price")
        model.createResource(FuhsenVocab.FACET_URI + "Country")
          .addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "country")
        model.createResource(FuhsenVocab.FACET_URI + "Location")
          .addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "location")
        model.createResource(FuhsenVocab.FACET_URI + "Condition")
          .addProperty(model.createProperty(FuhsenVocab.FACET_LABEL), "condition")
      case _ =>
    }

    Ok(RDFUtil.modelToTripleString(model, Lang.JSONLD))

  }

  def getFacetValues(uid: String, facet: String, entityType :String) = Action { request =>
    Logger.info("Facets values for : " + facet + " uid: "+uid+" entityType: "+entityType)

    GraphResultsCache.getModel(uid) match {
      case Some(model) =>
        Logger.info("Model size: "+model.size())
        val subModel = getFacetsModel(getFacetResultSet(facet, entityType, model))
        Logger.info("Facet SubModel size: "+subModel.size())
        Ok(RDFUtil.modelToTripleString(subModel, Lang.JSONLD))
      case None =>
        InternalServerError("Provided uid has not result model associated.")
    }

  }

  //Construct did not work, I do not understand why. Temporally we are executing select queries //getFacetResultSet
  private def getSubModelWithFacet(facet: String, entityType :String, model :Model) : Model = {

    entityType match {
      case "person" =>
        facet match {
          case "gender" =>
            Logger.info("Executing Construct Query")
            val query = QueryFactory.create(
              s"""
                 |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                 |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
                 |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                 |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                 |
                 |CONSTRUCT   {
                 |?p rdf:type fs:FacetValue .
                 |?p fs:count ?nelements .
                 |?p foaf:gender ?gender
                 |}
                 |WHERE {
                 |  ?p rdf:type foaf:Person .
                 |  ?p foaf:gender ?gender .
                 | { SELECT ?gender ( COUNT(?gender) as ?nelements ) { ?p foaf:gender ?gender } GROUP BY ?gender }
                 |  FILTER ( ?nelements > 0 )
                 |}
          """.stripMargin)
            val myModel = QueryExecutionFactory.create(query, model).execConstruct()
            myModel
        }
    }

  }

  private def getFacetResultSet(facet: String, entityType :String, model :Model) : ResultSet = {

    entityType match {
      case "person" =>
        facet match {
          case "gender" =>
            val query = QueryFactory.create(
              s"""
                 |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                 |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
                 |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                 |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                 |
                 |SELECT (SAMPLE(?gender) AS ?facet) (COUNT(?gender) as ?elems)
                 |WHERE {
                 |		?p rdf:type foaf:Person .
                 |    ?p foaf:gender ?gender .
                 |} GROUP BY ?gender
          """.stripMargin)
            val results = QueryExecutionFactory.create(query, model).execSelect()
            results
          case "birthday" =>
            val query = QueryFactory.create(
              s"""
                 |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                 |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
                 |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                 |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                 |
                 |SELECT (SAMPLE(?birthday ) AS ?facet) (COUNT(?birthday) as ?elems)
                 |WHERE {
                 |		?p rdf:type foaf:Person .
                 |    ?p fs:birthday ?birthday .
                 |} GROUP BY ?birthday
          """.stripMargin)
            val results = QueryExecutionFactory.create(query, model).execSelect()
            results
          case "location" =>
            val query = QueryFactory.create(
              s"""
                 |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                 |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
                 |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                 |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                 |
                 |SELECT (SAMPLE(?location) AS ?facet) (COUNT(?location) as ?elems)
                 |WHERE {
                 |		?p rdf:type foaf:Person .
                 |    ?p fs:location ?location .
                 |} GROUP BY ?location
          """.stripMargin)
            val results = QueryExecutionFactory.create(query, model).execSelect()
            results
          case "ocupation" =>
            val query = QueryFactory.create(
              s"""
                 |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                 |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
                 |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                 |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                 |
                 |SELECT (SAMPLE(?ocupation) AS ?facet) (COUNT(?ocupation) as ?elems)
                 |WHERE {
                 |		?p rdf:type foaf:Person .
                 |    ?p fs:ocupation ?ocupation .
                 |} GROUP BY ?ocupation
          """.stripMargin)
            val results = QueryExecutionFactory.create(query, model).execSelect()
            results
          case "livein" =>
            val query = QueryFactory.create(
              s"""
                 |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                 |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
                 |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                 |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                 |
                 |SELECT (SAMPLE(?livedAt) AS ?facet) (COUNT(?livedAt) as ?elems)
                 |WHERE {
                 |		?p rdf:type foaf:Person .
                 |    ?p fs:livedAt ?livedAt .
                 |} GROUP BY ?livedAt
          """.stripMargin)
            val results = QueryExecutionFactory.create(query, model).execSelect()
            results
          case "workat" =>
            val query = QueryFactory.create(
              s"""
                 |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                 |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
                 |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                 |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                 |
                 |SELECT (SAMPLE(?workAt) AS ?facet) (COUNT(?workAt) as ?elems)
                 |WHERE {
                 |		?p rdf:type foaf:Person .
                 |    ?p fs:workAt ?workAt .
                 |} GROUP BY ?workAt
          """.stripMargin)
            val results = QueryExecutionFactory.create(query, model).execSelect()
            results
          case "studyAt" =>
            val query = QueryFactory.create(
              s"""
                 |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                 |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
                 |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                 |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                 |
                 |SELECT (SAMPLE(?studyAt) AS ?facet) (COUNT(?studyAt) as ?elems)
                 |WHERE {
                 |		?p rdf:type foaf:Person .
                 |    ?p fs:studyAt ?studyAt .
                 |} GROUP BY ?studyAt
          """.stripMargin)
            val results = QueryExecutionFactory.create(query, model).execSelect()
            results
        }
      case "product" =>
        facet match {
          case "price" =>
            val query = QueryFactory.create(
              s"""
                 |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                 |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
                 |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                 |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                 |PREFIX gr: <http://purl.org/goodrelations/v1#>
                 |
                 |SELECT (SAMPLE(?price) AS ?facet) (COUNT(?price) as ?elems)
                 |WHERE {
                 |		?p rdf:type gr:ProductOrService .
                 |    ?p fs:priceLabel ?price
                 |} GROUP BY ?price
          """.stripMargin)
            val results = QueryExecutionFactory.create(query, model).execSelect()
            results
          case "country" =>
            val query = QueryFactory.create(
              s"""
                 |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                 |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
                 |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                 |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                 |PREFIX gr: <http://purl.org/goodrelations/v1#>
                 |
                 |SELECT (SAMPLE(?country) AS ?facet) (COUNT(?country) as ?elems)
                 |WHERE {
                 |		?p rdf:type gr:ProductOrService .
                 |    ?p fs:country ?country
                 |} GROUP BY ?country
          """.stripMargin)
            val results = QueryExecutionFactory.create(query, model).execSelect()
            results
          case "location" =>
            val query = QueryFactory.create(
              s"""
                 |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                 |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
                 |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                 |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                 |PREFIX gr: <http://purl.org/goodrelations/v1#>
                 |
                 |SELECT (SAMPLE(?location) AS ?facet) (COUNT(?location) as ?elems)
                 |WHERE {
                 |		?p rdf:type gr:ProductOrService .
                 |    ?p fs:location ?location
                 |} GROUP BY ?location
          """.stripMargin)
            val results = QueryExecutionFactory.create(query, model).execSelect()
            results
          case "condition" =>
            val query = QueryFactory.create(
              s"""
                 |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                 |PREFIX fs: <http://vocab.lidakra.de/fuhsen#>
                 |PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                 |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                 |PREFIX gr: <http://purl.org/goodrelations/v1#>
                 |
                 |SELECT (SAMPLE(?condition) AS ?facet) (COUNT(?condition) as ?elems)
                 |WHERE {
                 |		?p rdf:type gr:ProductOrService .
                 |    ?p fs:condition ?condition
                 |} GROUP BY ?condition
          """.stripMargin)
            val results = QueryExecutionFactory.create(query, model).execSelect()
            results
        }
    }
  }

  private def getFacetsModel(resultSet: ResultSet) : Model = {

    val facetsModel = ModelFactory.createDefaultModel()
    var addBlank = false
    while(resultSet.hasNext) {
      val result = resultSet.next
      val name = result.getLiteral("facet").getString
      val count = result.getLiteral("elems").getString
      val resource = facetsModel.createResource(FuhsenVocab.FACET_URI + name.trim.replace(" ", ""))
      resource.addProperty(facetsModel.createProperty(FuhsenVocab.FACET_VALUE), name)
      resource.addProperty(facetsModel.createProperty(FuhsenVocab.FACET_COUNT), count)
      addBlank = true
    }
    //Temporal solution since @graph does not appear when it is just one element
    if (addBlank) {
      val resource = facetsModel.createResource(FuhsenVocab.FACET_URI +"blankNode")
      resource.addProperty(facetsModel.createProperty(FuhsenVocab.FACET_VALUE), "blank")
      resource.addProperty(facetsModel.createProperty(FuhsenVocab.FACET_COUNT), "0")
    }

    facetsModel

  }



}
