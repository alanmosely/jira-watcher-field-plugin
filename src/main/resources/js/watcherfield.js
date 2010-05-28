/*
AJS.$('*').ajaxComplete(function(event, XMLHttpRequest, ajaxOptions) {
	var issueId = AJS.$(event.target).attr('rel');
	if(XMLHttpRequest.status == 200 && issueId !== undefined){
		var regex = new RegExp(contextPath + "/rest/api/(.+)/issues/"+issueId+"/watchers(.*)", "ig");
		if(regex.test(ajaxOptions.url)){
			AJS.$.ajax({ 
				url: contextPath + "/rest/watcherfield/latest/watchers?atl_token="+atl_token+"&issueId="+issueId,
				type: "GET",
				dataType: "json",
				success: function (response) {
					var html = '';
					if(response.watchers !== undefined){
						AJS.$(response.watchers).each(function(index, value){
							html += '<span class="tinylink">';
							html += '<a href="/jira/secure/ViewProfile.jspa?name='+value.username+'" id="multiuser_cf_'+value.username+'">'+value.displayName+'</a>';
							html += '</span>, ';
						});
					}
					AJS.$(response.fieldIds).each(function(index, value){
						AJS.$('#'+value+'-field').html(html);
					});
				},
				error: function(XMLHttpRequest, textStatus, errorThrown){
					console.log('Error in REST call to get watcher: ' + XMLHttpRequest.status + ',' + XMLHttpRequest.statusText);
					console.log(XMLHttpRequest);
				}
			});
		}
	}
});
*/