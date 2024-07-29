/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.romys.tiktok.processors;

import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.io.OutputStreamCallback;           

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Tags({"romys", "tiktok", "profile", "extract"})
@CapabilityDescription("Provide a description")
public class TikTokUserExtractor extends AbstractProcessor {
    private OkHttpClient client = new OkHttpClient();
    private ObjectMapper objectMapper = new ObjectMapper();

    public static final PropertyDescriptor USERNAME = new PropertyDescriptor
        .Builder()
        .name("username")       
        .displayName("Username")
        .description("Username of tiktok user")
        .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
        .required(false)
        .build();

    private static final Relationship REL_SUCCESS = new Relationship.Builder()
        .name("success")
        .description("Successfully processed data.")
        .build();

    private static final Relationship REL_FAILURE = new Relationship.Builder()
        .name("failure")
        .description("Failed to process data.")
        .build();      

    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;

    // public static void main(String[] args) throws JsonMappingException, JsonProcessingException, IOException {
    //     System.out.println(new TikTokUserExtractor().getUserProfile("fall.for.yo"));
    // }

    @Override
    protected void init(final ProcessorInitializationContext context) {
        descriptors = new ArrayList<>();
        descriptors.add(USERNAME);
        descriptors = Collections.unmodifiableList(descriptors);

        relationships = new HashSet<>();
        relationships.add(REL_FAILURE);
        relationships.add(REL_SUCCESS);
        relationships = Collections.unmodifiableSet(relationships);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return descriptors;
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) {

    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) {
        FlowFile flowFile = session.get();
        if ( flowFile == null ) return;

        try {
            final String username = context.getProperty(USERNAME).getValue();
            String content = this.getUserProfile(username);
            final FlowFile outFlowFile = session.write(flowFile, new OutputStreamCallback() {
                @Override
                public void process(OutputStream out) throws IOException {
                    out.write(content.getBytes(StandardCharsets.UTF_8));
                }
            });

            session.transfer(outFlowFile, REL_SUCCESS);
        } catch (Exception e) {
            getLogger().error("Failed to process due to {}", new Object[]{e.getMessage()}, e);
            session.transfer(flowFile, REL_FAILURE);
        }
    }

    private String parseData(String content) throws JsonMappingException, JsonProcessingException{
        Document document = Jsoup.parse(content);
        JsonNode jsonNode = this.objectMapper.readTree(
            document.select("#__UNIVERSAL_DATA_FOR_REHYDRATION__").html()
        );
        return jsonNode.get("__DEFAULT_SCOPE__").get("webapp.user-detail").toString();
    }   

    public String getUserProfile(String username) throws JsonMappingException, JsonProcessingException, IOException{
        Request request = new Request.Builder()
            .url(
                String.format("https://www.tiktok.com/@%s", username)
            )
            .build();           
    
        Response response = this.client.newCall(request).execute();
        return this.parseData(response.body().string());
    }       
}
