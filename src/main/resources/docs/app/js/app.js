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
      })
      .otherwise({
        redirectTo: '/general'
      });
  });
