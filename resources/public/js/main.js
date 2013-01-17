"use strict";

var SubmitFormModel = Backbone.Model.extend({

});

var SubmitFormView = Backbone.View.extend({
	events: {
            "click #submitButton": "submitClicked"
    },

	initialize: function (attrs) {
		this.template = _.template(attrs.template);
	},

	render: function() {
		this.$el.html(this.template(this.model.toJSON()));
	},

	submitClicked: function() {
		var myForm = this.$('#submitForm');
		var valid = myForm[0].checkValidity();
		if (!valid) {
			return true;
		}

		return false;
	}

});

$(function() {
	var fv=new SubmitFormView({
		model: new SubmitFormModel,
		el: $("#main"),
		template: $("#submitFormTemplate").html()
	})
	fv.render();
});