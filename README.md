<h1 align="center">Spring Boot Starter Requery GraphQL</h1>

<p align="center">
    <img src="./codexio-logo.png" width="555" height="90"/>
    <br/>
    <em>
        May your queries be requested.
    </em>
</p>

<div align="center">

[![Maven Central](https://img.shields.io/maven-central/v/bg.codexio.springframework.boot/spring-boot-starter-requery-graphql?color=EE5A9C)](https://central.sonatype.com/artifact/bg.codexio.springframework.data.jpa.requery/requery-core)
[![Build](https://github.com/CodexioLtd/spring-boot-starter-requery-graphql/actions/workflows/maven.yml/badge.svg)](https://github.com/CodexioLtd/spring-requery/actions/workflows/maven.yml)
[![Coverage](https://codecov.io/github/CodexioLtd/spring-requery/graph/badge.svg?token=013OEUIYWI)](https://codecov.io/github/CodexioLtd/spring-boot-starter-requery-graphql)
[![License](https://img.shields.io/github/license/CodexioLtd/spring-boot-starter-requery-graphql.svg)](./LICENSE)

</div>

<hr/>

## Preambule

The Spring <img src="https://spring.io/img/logos/spring-initializr.svg" width="18px" height="18px"/> Boot starter
provides <b>additional</b> autoconfiguration for the [Spring Boot Starter Requery by Codexio](https://github.com/CodexioLtd/spring-boot-starter-requery)
to integrate with the GraphQL protocol. This starter automatically sets up all necessary beans, allowing you to 
directly leverage `GraphQLHttpFilterAdapter` object as an additional adapter.

## Table of Contents

* [Preamble](#preamble)
* [Table of Contents](#table-of-contents)
* [Features](#features)
* [Quick Start](#quick-start)
* [Configuration Details](#configuration-details)
* [Usage](#usage)
    * [GraphQL Filter Configuration](#graphql-filter-configuration)
    * [Example GraphQL Queries](#example-graphql-queries)
* [Shortcomings](#shortcomings)
* [Contributing](#contributing)
* [License](#license)

## Features

* **GraphQL Query Support:** Enables filtering with traditional GraphQL queries and complex GraphQL filters (with a
  similar body to the one used in the base requery library), both through JSON-based filter parsing, served via HTTP (
  either GET or POST) .
* **Automatic Configuration:** Sets up the `GraphQLComplexFilterAdapter` and `GraphQLHttpFilterAdapter` with minimal
  configuration.
* **Seamless Integration with Spring Boot Starter Requery:** Extends dynamic query capabilities provided by Spring Boot
  Starter Requery, making it easy to integrate GraphQL with Spring Data JPA.

## Quick Start

1. **Include Dependency:** Add the `Spring Boot Starter Requery GraphQL` dependency to your Spring Boot project:

   ```xml
   <dependency>
       <groupId>bg.codexio.springframework.boot</groupId>
       <artifactId>spring-boot-starter-requery-graphql</artifactId>
       <version>1.0.3-SNAPSHOT</version>
   </dependency>
   ```
2. **Enable GraphQL Filter Support:** Configure your application properties to enable request body caching for GraphQL:

   ```properties
   codexio.requery.adapters.graphql.supports.check-body=true
   ```

3. **Define GraphQL Endpoint:** Define an endpoint in your GraphQL schema to leverage the filtering capabilities
   provided by this starter. Use the `GraphQLHttpFilterAdapter` in your resolver to handle filter criteria.

## Configuration Details

The **Spring Boot Starter Requery GraphQL** auto-configures the following components:

* `GraphQLComplexFilterAdapter`: Adapts complex JSON-based filter structures within GraphQL queries.
* `GraphQLHttpFilterAdapter`: Handles GraphQL-specific HTTP requests, adapting filters based on configured request
  properties.
* `RequestCachingFilterConfig`: Caches HTTP request bodies to allow multiple reads, necessary for parsing GraphQL query
  content more than once within the request lifecycle.

### Setting up Request Caching

Ensure `RequestCachingFilterConfig` is enabled to allow multiple reads of the request body. This setup is conditionally
applied based on the `codexio.requery.adapters.graphql.supports.check-body` property, as shown in the Quick Start
section.

## Usage

### GraphQL Filter Configuration

After setting up the **Spring Boot Starter Requery GraphQL** in your project, you can use the
`GraphQLComplexFilterAdapter` and `GraphQLHttpFilterAdapter` to dynamically handle filter parameters in your GraphQL
queries. These adapters parse JSON-based filter definitions embedded in GraphQL requests and convert them into JPA
`Specification` objects.

### Properties definable in you application.properties file

* `codexio.requery.adapters.graphql.supports.url-pattern` - here you could put this value in order to get all mappings
  starting with /graphql

```properties
.*\\/graphql.*
```

* `codexio.requery.adapters.graphql.supports.check-body` and ``codexio.requery.adapters.graphql.supports.inclusive`` are
  optional and by setting them to `true` you will have a validation of you request body and if *inclusive* is set to
  true as well, both the url and the body have to be valid as well

* `codexio.requery.adapters.active` is a property that could control which adapters are instantiated when the
  application is started (*this includes custom adapters that implement the `HttpFilterAdapter` interface, if you want
  to create one*). If it is not specified, all adapters implementing the `HttpFilterAdapter` will be instantiated and
  traversed for their `supports` function.

### Example GraphQL Queries

Below are examples of GraphQL queries that support filtering with and without variables. These queries use JSON-based
filters, which are converted to JPA specifications to execute dynamic queries. Keep in mind that in order to specify the
filter object for the GET HTTP Requests use ``filter=`` and for variables encoded value ``variables=``

*\*Note*: For construction of the simple queries the convention is as follows nameOfField_operation.

<br>

#### Simple Query with Variables:

The HTTP POST Request body:
```json
{
  "query": "{user(firstName: \"$firstName\", address: { zip_in: $zip }) { id name friends { id city } } }",
  "variables" : "{\"$firstName\" : \"John\", \"$zip\":[\"10001\", \"77001\"]}"
}
```

The HTTP GET URL:

```
http://localhost:8080/users/graphql?query=%7B%0A%20user%28firstName%3A%20%22%24firstName%22%2C%20address%3A%20%7B%20zip_in%3A%20%24zip%20%7D%29%20%7B%20id%20name%20friends%20%7B%20id%20city%20%7D%20%7D%20%7D&variables=%7B%22%24firstName%22%20%3A%20%22John%22%2C%20%22%24zip%22%3A%5B%2210001%22%2C%20%2277001%22%5D%7D
```

<br>

#### Simple Query without Variables:

The HTTP POST Request body:
```json
{
  "query": "{ user(firstName: \"John\", address: { zip_in: [\"10001\"] }) { id name friends { id city } } }"
}
```

The HTTP GET URL:

```
http://localhost:8080/users/graphql?query=%7B%0A%20user%28firstName%3A%20%22John%22%2C%20address%3A%20%7B%20zip_in%3A%20%5B10001%2C%2077001%5D%20%7D%29%20%7B%20id%20name%20friends%20%7B%20id%20city%20%7D%20%7D%20%7D
```

<br>

#### Complex Query with Variables:

The HTTP POST Request body:
```json
{
  "query": "users(filter: {\"groupOperations\": [{\"field\": \"email\", \"operation\": \"CONTAINS\", \"value\": \"$emailContains\"}], \"nonPriorityGroupOperators\": [\"AND\"], \"rightSideOperands\": {\"unaryGroupOperator\": \"AND\", \"unaryGroup\": {\"groupOperations\": [{\"field\": \"firstName\", \"operation\": \"IN\", \"value\": [\"$firstName\"]}, {\"field\": \"lastName\", \"operation\": \"NOT_EMPTY\", \"value\": \"$lastName\"}], \"nonPriorityGroupOperators\": [\"OR\"], \"rightSideOperands\": {\"unaryGroupOperator\": \"AND\", \"unaryGroup\": {\"groupOperations\": [{\"field\": \"age\", \"operation\": \"GTE\", \"value\": 25}], \"nonPriorityGroupOperators\": []}}}}}) { id firstName lastName address { id city } } ",
  "variables" : "{\"$emailContains\":\"5@\", \"$lastName\":\"Doe\", \"$firstName\":[\"John\", \"David\"]}"
}
```

The HTTP GET URL:

```
http://localhost:8080/users/graphql?query=users%28filter%3A%20%7B%22groupOperations%22%3A%20%5B%7B%22field%22%3A%20%22email%22%2C%20%22operation%22%3A%20%22CONTAINS%22%2C%20%22value%22%3A%20%22%24emailContains%22%7D%5D%2C%20%22nonPriorityGroupOperators%22%3A%20%5B%22AND%22%5D%2C%20%22rightSideOperands%22%3A%20%7B%22unaryGroupOperator%22%3A%20%22AND%22%2C%20%22unaryGroup%22%3A%20%7B%22groupOperations%22%3A%20%5B%7B%22field%22%3A%20%22firstName%22%2C%20%22operation%22%3A%20%22IN%22%2C%20%22value%22%3A%20%5B%22%24firstName%22%5D%7D%2C%20%7B%22field%22%3A%20%22lastName%22%2C%20%22operation%22%3A%20%22NOT_EMPTY%22%2C%20%22value%22%3A%20%22%24lastName%22%7D%5D%2C%20%22nonPriorityGroupOperators%22%3A%20%5B%22OR%22%5D%2C%20%22rightSideOperands%22%3A%20%7B%22unaryGroupOperator%22%3A%20%22AND%22%2C%20%22unaryGroup%22%3A%20%7B%22groupOperations%22%3A%20%5B%7B%22field%22%3A%20%22age%22%2C%20%22operation%22%3A%20%22GTE%22%2C%20%22value%22%3A%2025%7D%5D%2C%20%22nonPriorityGroupOperators%22%3A%20%5B%5D%7D%7D%7D%7D%7D%29%20%7B%20id%20firstName%20lastName%20address%20%7B%20id%20city%20%7D%20%7D%20&variables=%7B%22%24emailContains%22%3A%225%40%22%2C%20%22%24lastName%22%3A%22Doe%22%2C%20%22%24firstName%22%3A%5B%22John%22%2C%20%22David%22%5D%7D
```

<br>

#### Complex Query without Variables:

```json
{
  "query": "users(filter: {\"groupOperations\": [{\"field\": \"email\", \"operation\": \"CONTAINS\", \"value\": \"5@\"}], \"nonPriorityGroupOperators\": [\"AND\"], \"rightSideOperands\": {\"unaryGroupOperator\": \"AND\", \"unaryGroup\": {\"groupOperations\": [{\"field\": \"firstName\", \"operation\": \"IN\", \"value\": [\"John\", \"David\"]}, {\"field\": \"lastName\", \"operation\": \"NOT_EMPTY\", \"value\": \"Doe\"}], \"nonPriorityGroupOperators\": [\"OR\"], \"rightSideOperands\": {\"unaryGroupOperator\": \"AND\", \"unaryGroup\": {\"groupOperations\": [{\"field\": \"age\", \"operation\": \"GTE\", \"value\": 25}], \"nonPriorityGroupOperators\": []}}}}}) { id firstName lastName address { id city } } "
}
```

The HTTP GET URL:

```
http://localhost:8080/users/graphql?query=users%28filter%3A%20%7B%22groupOperations%22%3A%20%5B%7B%22field%22%3A%20%22email%22%2C%20%22operation%22%3A%20%22CONTAINS%22%2C%20%22value%22%3A%20%22%24emailContains%22%7D%5D%2C%20%22nonPriorityGroupOperators%22%3A%20%5B%22AND%22%5D%2C%20%22rightSideOperands%22%3A%20%7B%22unaryGroupOperator%22%3A%20%22AND%22%2C%20%22unaryGroup%22%3A%20%7B%22groupOperations%22%3A%20%5B%7B%22field%22%3A%20%22firstName%22%2C%20%22operation%22%3A%20%22IN%22%2C%20%22value%22%3A%20%5B%22%24firstName%22%5D%7D%2C%20%7B%22field%22%3A%20%22lastName%22%2C%20%22operation%22%3A%20%22NOT_EMPTY%22%2C%20%22value%22%3A%20%22%24lastName%22%7D%5D%2C%20%22nonPriorityGroupOperators%22%3A%20%5B%22OR%22%5D%2C%20%22rightSideOperands%22%3A%20%7B%22unaryGroupOperator%22%3A%20%22AND%22%2C%20%22unaryGroup%22%3A%20%7B%22groupOperations%22%3A%20%5B%7B%22field%22%3A%20%22age%22%2C%20%22operation%22%3A%20%22GTE%22%2C%20%22value%22%3A%2025%7D%5D%2C%20%22nonPriorityGroupOperators%22%3A%20%5B%5D%7D%7D%7D%7D%7D%29%20%7B%20id%20firstName%20lastName%20address%20%7B%20id%20city%20%7D%20%7D%20&variables=%7B%22%24emailContains%22%3A%225%40%22%2C%20%22%24lastName%22%3A%22Doe%22%2C%20%22%24firstName%22%3A%5B%22John%22%2C%20%22David%22%5D%7D
```

These examples demonstrate the use of JSON-based filters within GraphQL queries, enabling advanced filtering
functionality that can dynamically handle complex query structures.

## Shortcomings

There are some setbacks we have faced developing the product, but we are more than opened to recommendations or any help
regarding these issues.

### *Fields dynamic choice*

Unfortunately for now, the possibility for choosing which are the only fields you want included in the response entity
is not achieved.

### *Filtration of Collection fields*

Currently, the filtration of the fields (if they are Collections) is not supported.

### *Complex filter object is not purely GraphQL*

The complex filtration request object is just a typical one for the normal Requery library supported objects. Currently
complex filtration similar to the simple one is not possible.

## Contributing

This project could use a support and contributors are very welcomed. If you feel that something has to be
changed or a bug to be fixed, you can report
a [new issue](https://github.com/CodexioLtd/spring-boot-starter-requery-graphql/issues/new), and
we can take care of it.

If you want to submit directly a code fix, we will be more than glad to see it. Fork the repository and start a clean
branch out of the version you want to patch. When you are finished, make sure all your tests are passing and the
coverage remains in decent level by executing `mvn clean test jacoco:report -Pmvn-deploy`.

Please use the [code style](./codestyle.xml)
in the project root folder. If your IDE does not support it, we strongly encourage you just to follow
the code styling in the rest of the classes and methods.

After all, your tests are passing and the coverage seems good to you, create a
[pull request](https://github.com/CodexioLtd/spring-boot-starter-requery-graphql/compare). We will review the request 
and either leave some meaningful suggestions back or maybe merge it and release it with the next release.

...

## License

Copyright 2024 [Codexio Ltd.](https://codexio.bg)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
