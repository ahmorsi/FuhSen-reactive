
var width = 960,
    height = 650,r=50;

var rectW = 2*r;
var rectH = r;

var typeToColor = { 
    "PERSON" : "#ffcc66",
"LOC" : "#99ddff",
"ORG" : "#85e085",
"LITERAL" : "#fff"};

    var force = d3.layout.force()
    .charge(-600)
    .linkDistance(240)
    .linkStrength(0.5)
    .gravity(0.04)
    .size([width, height]);

var svg = d3.select("#graph").append("svg")
    .style("background","white")
    .attr("width", width)
    .attr("height", height);

// define arrow markers for graph links
svg.append('svg:defs').append('svg:marker')
    .attr('id', 'end-arrow')
    .attr('viewBox', '0 -5 10 10')
    .attr('refX', 6)
    .attr('markerWidth', 8)
    .attr('markerHeight', 8)
    .attr('orient', 'auto')
  .append('svg:path')
    .attr('d', 'M0,-5L10,0L0,5')
    .attr('fill', 'red');

svg.append('svg:defs').append('svg:marker')
    .attr('id', 'start-arrow')
    .attr('viewBox', '0 -5 10 10')
    .attr('refX', 4)
    .attr('markerWidth', 10)
    .attr('markerHeight', 10)
    .attr('orient', 'auto')
  .append('svg:path')
    .attr('d', 'M10,-5L0,0L10,5')
    .attr('fill', 'red');
    
d3.json("/assets/data/graph.json", function(error, graph) {
  if (error) throw error;

 // path (link) group
 var path = svg.append('svg:g').selectAll('path')
                    .data(graph.links)
                    .enter().append('svg:path')
                    .attr('class', 'link')
                    .attr("id",function(d,i) {return 'edge'+i;})
                    .style('marker-end', 'url(#end-arrow)');

   
    var edgelabels = svg.selectAll(".edgelabel")
        .data(graph.links)
        .enter()
        .append('text')
        .style("pointer-events", "none")
        .attr({'class':'edgelabel',
               'id':function(d,i){return 'edgelabel'+i;},
               'dx':80,
               'dy':0,
               'font-size':10,
               'fill':'#aaa'});

    edgelabels.append('textPath')
        .attr('xlink:href',function(d,i) {return '#edge'+i;})
        .text(function(d,i){return d.label;});
        
  var nodeContainer = svg.append('svg:g').selectAll(".node")
      .data(graph.nodes)
    .enter().append('svg:g');
 
 nodeContainer.filter(function(d){ return d.type ==  "LITERAL"; })
                        .append('svg:rect')
                        .attr("width", rectW)
                        .attr("height", rectH)
                      .attr("class", "node")
                      .style("fill", function(d) { return typeToColor[d.type]; })
                      .style("stroke-dasharray","5,5")
                      .call(force.drag);
 
 nodeContainer
      .filter(function(d){ return d.type !=  "LITERAL"; })
      .append('svg:circle')
      .attr("class", "node")
      .attr("r", r)
      .style("fill", function(d) { return typeToColor[d.type]; })
      .call(force.drag);
  
  nodeContainer.filter(function(d){ return d.type !=  "LITERAL"; })
         .append("svg:text")
        .text(function (d) {
        return d.name;
    }).style("font-size", function(d) { return Math.min(2 * r, (2 * r - 8) / this.getComputedTextLength() * 20) + "px"; })
      .attr("dy", ".35em");                          
  
  nodeContainer.filter(function(d){ return d.type ==  "LITERAL"; })
               .append("svg:text")
               .style("alignment-baseline","central") 
               .attr("x",rectW/2)
               .attr("y",rectH/2)
                .text(function (d) {
        return d.name;
    }).style("font-size", function(d) { return Math.min(rectW, (rectW - 8) / this.getComputedTextLength() * 20) + "px"; });
    
  force.on("tick", function() {
    path.attr('d', function(d) {
    var startX = d.source.x,startY = d.source.y,endX= d.target.x,endY = d.target.y;
    if(d.source.type == "LITERAL"){
        startX += rectW/2;
        startY += rectH/2;
    }
    if(d.target.type == "LITERAL"){
        endX += rectW/2;
        endY += rectH/2;
    }
    var diagonalDist = Math.sqrt(rectW/2 * rectW/2 + rectH/2 * rectH/2);
    var deltaX = endX - startX,
        deltaY = endY - startY,
        dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY),
        normX = deltaX / dist,
        normY = deltaY / dist,
        sourcePadding = (d.source.type != "LITERAL" ? r : diagonalDist),
        targetPadding = (d.target.type != "LITERAL" ? r + 5 : diagonalDist),
        sourceX = startX + (sourcePadding * normX),
        sourceY = startY + (sourcePadding * normY),
        targetX = endX - (targetPadding * normX),
        targetY = endY - (targetPadding * normY);
        
    return 'M' + sourceX + ',' + sourceY + 'L' + targetX + ',' + targetY;
  });

    nodeContainer.attr('transform', function(d) {
    return 'translate(' + d.x + ',' + d.y + ')';
  });
  
  edgelabels.attr('transform',function(d,i){
            if (d.target.x<d.source.x){
                bbox = this.getBBox();
                rx = bbox.x+bbox.width/2;
                ry = bbox.y+bbox.height/2;
                return 'rotate(180 '+rx+' '+ry+')';
                }
            else {
                return 'rotate(0)';
                }
        });
  });
  
  force
      .nodes(graph.nodes)
      .links(graph.links)
      .start();
});