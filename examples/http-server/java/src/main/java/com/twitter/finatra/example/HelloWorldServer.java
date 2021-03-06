package com.twitter.finatra.example;

import java.util.Collection;
import java.util.Collections;

import scala.reflect.ManifestFactory;

import com.google.inject.Module;

import com.twitter.app.Flaggable;
import com.twitter.finatra.example.controllers.HelloWorldController;
import com.twitter.finatra.example.exceptions.HelloWorldExceptionMapper;
import com.twitter.finatra.example.filters.AppendToHeaderFilter;
import com.twitter.finatra.example.modules.MagicNumberModule;
import com.twitter.finatra.http.AbstractHttpServer;
import com.twitter.finatra.http.filters.CommonFilters;
import com.twitter.finatra.http.routing.HttpRouter;

public class HelloWorldServer extends AbstractHttpServer {

  public HelloWorldServer() {
    createFlag(
        /* name = */ "magic.number",
        /* default = */ 55,
        /* help = */ "This is a magic number.",
        /* flaggable = */ Flaggable.ofJavaInteger());
  }

  @Override
  public Collection<Module> javaModules() {
    return Collections.singletonList(new MagicNumberModule());
  }

  @Override
  public void configureHttp(HttpRouter router) {
    router
        .filter(CommonFilters.class)
        .filter(new AppendToHeaderFilter("foo", "1"))
        .add(HelloWorldController.class)
        .register(ManifestFactory.classType(CatMessageBodyReader.class))
        .register(ManifestFactory.classType(DogMessageBodyWriter.class))
        .exceptionMapper(HelloWorldExceptionMapper.class);
  }
}
