'use strict';

var theModule = angular.module('gservDocsApp', [
      'ngResource','ngRoute','ngLocale',
      'ui.bootstrap'    ]);

theModule.config(function ($routeProvider) {
    $routeProvider
      .when('/general', {
        templateUrl: 'views/general.html',
        controller: "GServGeneralDocsCtrl"
      }).when('/framework', {
        templateUrl: 'views/framework.html',
        controller: "GServFrameworkDocsCtrl"
      }).when('/standalone', {
        templateUrl: 'views/standalone.html',
        controller: "GServStandaloneDocsCtrl"
      }).when('/examples/basicauth', {
        templateUrl: 'views/examples/basicAuth.html',
        controller: "ExampleCtrl"
      }).when('/examples/cors', {
        templateUrl: 'views/examples/cors.html',
        controller: "ExampleCtrl"
      })
      .otherwise({
        redirectTo: '/general'
      });
  });
