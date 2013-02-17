// Foreach oldquark, output a YAML -- NOTE THAT LOTS OF TIDYING IS NEEDED AFTER!
(
Quarks.local.quarks.do{|q|
	var qi = q.info.deepCopy;
	qi[\method]='svn';
	qi['uri'] = Quarks.repos.url +/+ q.info[\path];
	qi[\path]=nil;
	qi[\authorurl]=qi[\url];
	qi[\url]=nil;

	qi[\dependencies] = qi[\dependencies].collect{|dep|
		if(dep.isString){(name:dep)}{(name: dep.key, version: dep.value)}
	};

	if(qi['version'].notNil){ qi['version']=(qi['version']:{})};
	"%: %,\n".format(q.name, JSON.stringify(qi)).postln;
}; "";
)

