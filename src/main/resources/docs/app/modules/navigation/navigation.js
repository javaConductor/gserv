/**
 * Created by Kevin Musselman.
 */
'use strict';

var app = angular.module('NavigationBar', []);

/* Directives */

app.directive('navMenu', ['$parse', '$compile', function($parse, $compile) {
        return {
            restrict: 'C', //Element
            scope:true,
            link: function (scope, element, attrs)
            {
                scope.selectedNode = null;

                scope.$watch( attrs.menuData, function(val)
                {
                    var template = angular.element('<ul id="parentTreeNavigation"><li ng-repeat="node in ' + attrs.menuData + '" ng-class="{active:node.active && node.active==true, \'has-dropdown\': !!node.children && node.children.length}"><a ng-href="{{node.href}}" ng-click="{{node.click}}" target="{{node.target}}" >{{node.text}}</a><sub-navigation-tree></sub-navigation-tree></li></ul>');
                    var linkFunction = $compile(template);
                    linkFunction(scope);
                    element.html(null).append( template );

                }, true );
            }
        };
    }])
    .directive('subNavigationTree', ['$compile', function($compile)
    {
        return {
            restrict: 'E', //Element
            scope:true,
            link: function (scope, element, attrs)
            {
                scope.tree = scope.node;

                if(scope.tree.children && scope.tree.children.length )
                {
                    var template = angular.element('<ul class="dropdown "><li ng-repeat="node in tree.children" node-id={{node.' + attrs.nodeId + '}}  ng-class="{active:node.active && node.active==true, \'has-dropdown\': !!node.children && node.children.length}"><a ng-href="{{node.href}}" ng-click="{{node.click}}" target="{{node.target}}" ng-bind-html-unsafe="node.text"></a><sub-navigation-tree tree="node"></sub-navigation-tree></li></ul>');

                    var linkFunction = $compile(template);
                    linkFunction(scope);
                    element.replaceWith( template );
                }
                else
                {
                    element.remove();
                }
            }
        };
    }]);
