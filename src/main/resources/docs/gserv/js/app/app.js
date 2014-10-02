'use strict';

var theModule = angular.module('gservEventLogger', [
      'ngResource', 'ngRoute'
    ]);

function EventLoggerController($scope){
    console.log("EventLoggerController() created.");

    $scope.model = {
        events : [],
        filter : {}
    }

    $scope.eventClass = function(evt){
        if ( evt.topic.indexOf("/error") > 0 ){
            return "error";
        }
        else{
            if ( evt.topic.indexOf("/debug") > 0 ){
                  return "debug";
            }
            else{
                if ( evt.topic.indexOf('g$$/') === 0 ){
                    return "message";
                }
                else
                    return "";
                }
        }
    };

    $scope.getEvents = function(){
        var url = "/log";

        $.ajax({
            url: url,
        })
        .done(function( data ) {
            $scope.$apply(function(){
                $scope.model.events = (eval(data).reverse());
            });
        })
        .fail(function( jqXHR, textStatus, errorThrown ) {
            alert( "ERROR:" + jqXHR.statusText + ":" + jqXHR.status+":" + jqXHR.responseText )
        });
    }
    $scope.getEvents()
}
EventLoggerController.$inject = ["$scope"];

theModule.config(function ($routeProvider) {
    $routeProvider
      .when('/', {
        templateUrl: '/gserv/views/eventLoggerPlugin/index.html',
        controller: "EventLoggerController"
      })
      .otherwise({
        redirectTo: '/'
      });
  });

theModule.directive('jsonTree', function() {
    return {
        restrict: 'E',
        scope: {
              jsonObject: '=data'
            },
        template: '<div> </div>',
        link: function(scope, element, attrs){
                ///TODO call jquery on element HERE !!!
                $(element).JSONView(scope.jsonObject, {collapsed: true})
        }
    };
  });
