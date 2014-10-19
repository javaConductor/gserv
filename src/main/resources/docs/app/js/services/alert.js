/**
 * Created by lcollins on 6/28/14.
 */
var svc = angular.module('utilityServices',[])

svc.factory('AlertService', function($rootScope) {
    var alertService = {};

    // create an array of alerts available globally
    $rootScope.alerts = [];

    alertService.error = function(msg) {
        $rootScope.alerts.push({'type': 'error', 'msg': msg});
    };

    alertService.warning = function(msg) {
        $rootScope.alerts.push({'type': 'warning', 'msg': msg});
    };

    alertService.success = function(msg) {
        $rootScope.alerts.push({'type': 'success', 'msg': msg});
    };

    alertService.info = function(msg) {
        $rootScope.alerts.push({'type': 'info', 'msg': msg});
    };

    alertService.closeAlert = function(index) {
        $rootScope.alerts.splice(index, 1);
    };
    return alertService;
});
