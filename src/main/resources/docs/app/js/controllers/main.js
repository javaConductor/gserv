'use strict';

angular.module('gservDocsApp')
    .controller('MainController', ['$location','$scope', '$rootScope',  'AlertService',
        function ($location,$scope, $rootScope, alertService) {

            $scope.menuItems = [
                {
                    title: "GServ Documentation",
                    view: 'docs/index.html',
                    children : [
                        {
                            title: "GServ API",
                            view: 'docs/api/index.html',
                            children : []
                        },    {
                            title: "GServ Usage",
                            view: 'docs/usage/index.html',
                            children : []
                        },    {
                            title: "GServ Plugins",
                            view: 'docs/plugins/index.html',
                            children : []
                        }
                    ]
                }
            ]

            $scope.gotoPage = function(page){
                $location.path(page)
            }
            $scope.errorMessages = []
            $scope.menu = [
                {text: 'HOME', href:'#/' },
                {text: 'SEARCH', href:'#search' },
                {text: 'VERSES', href:'#verses' }
            ];

            $scope.closeAlert = function(idx){
                alertService.closeAlert(idx);
            }

        }]);
