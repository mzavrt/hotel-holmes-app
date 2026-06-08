package com.holmeshotel.integration.camel;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * XML Webhook Route
 * * Exposes a REST endpoint /api/camel/webhook/xml that accepts XML payloads,
 * keeps them as raw XML, uses XPath (via bean) to extract booking data,
 * normalizes key names, and forwards to the Camunda starter route.
 */
@Component
public class XmlWebhookRoute extends RouteBuilder {

    @Autowired
    private XPathDataExtractor xPathExtractor;

    @Override
    public void configure() throws Exception {

        // Configure REST endpoint for XML webhook
        rest()
            .post("/webhook/xml")
            .description("Receive booking requests via XML")
            .consumes("application/xml")
            .produces("application/json")
            // VYPNUTO BINDING: Chceme syrové XML, protože používáme XPath!
            .bindingMode(RestBindingMode.off) 
            .to("direct:extractXmlData");

        // Route: Receive XML, extract data, and normalize
        from("direct:extractXmlData")
            .routeId("xmlWebhookRoute")
            .log("📥 [XML Gateway] Received booking request from XML endpoint")
            .log("Request body: ${body}")
            
            // EIP: Content Enricher - use bean to extract data from XML using @XPath
            .bean(xPathExtractor, "extractBookingData")
            
            .log("✅ [XML Gateway] Data extracted via XPath. Keys normalized successfully")
            
            // EIP: Recipient List - forward to Camunda starter
            .to("direct:startCamunda")
            
            .end();
    }
}