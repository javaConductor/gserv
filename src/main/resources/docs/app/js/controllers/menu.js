'use strict';

angular.module('gservDocsApp')
    .controller('MenuController', ['$location','$scope', '$rootScope',  'AlertService',
        function ($location,$scope, $rootScope, alertService) {

            $scope.showView = this.showView = function(menuItem){
                $location.url(menuItem.view, false)
            };
            $scope.menuItems = [
                {
                    title: "GServ Documentation",
                    view: 'views/docs/index.md',
                    children : [
                        {
                            title: "GServ API",
                            view: 'views/docs/api/index.html',
                            children : []
                        },    {
                            title: "GServ Usage",
                            view: 'views/docs/usage/index.html',
                            children : []
                        },    {
                            title: "GServ Plugins",
                            view: 'views/docs/plugins/index.html',
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
