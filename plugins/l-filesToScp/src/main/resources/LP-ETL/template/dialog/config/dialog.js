define([], function () {
    "use strict";

    const DESC = {
        "$namespace": "http://plugins.linkedpipes.com/ontology/l-filesToScp#",
        "$type": "Configuration",
        "$control": {
            "$predicate": "auto"
        },
        "host": {
            "$type": "str",
            "$label": "Host address"
        },
        "port": {
            "$type": "int",
            "$label": "Port number"
        },
        "directory": {
            "$type": "str",
            "$label": "Target directory"
        },
        "createDirectory": {
            "$type": "bool",
            "$label": "Create target directory"
        },
        "userName": {
            "$type": "str",
            "$label": "User name"
        },
        "password": {
            "$type": "str",
            "$label": "Password"
        }
    };

    function controller($scope, $service) {

        if ($scope.dialog === undefined) {
            $scope.dialog = {};
        }

        const dialogManager = $service.v1.manager(DESC, $scope.dialog);

        $service.onStore = function () {
            dialogManager.save();
        };

        dialogManager.load();

    }

    controller.$inject = ['$scope', '$service'];
    return controller;
});
