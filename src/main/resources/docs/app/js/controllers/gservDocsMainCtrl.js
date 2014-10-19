
function GServDocsMainCtrl($scope, $location){

    $scope.menuItems = []
    $scope.gotoPage = function(page){
        $location.path(page)
    }

}

GServDocsMainCtrl.$inject = ["$scope", "$location" ]


