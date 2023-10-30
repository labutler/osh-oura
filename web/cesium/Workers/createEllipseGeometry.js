/*! For license information please see createEllipseGeometry.js.LICENSE.txt */
define(["./Matrix3-edb29a7e","./defaultValue-135942ca","./EllipseGeometry-4118825e","./Math-a304e2d6","./Transforms-3ea76111","./Matrix2-7a2bab7e","./RuntimeError-f0dada00","./combine-462d91dd","./ComponentDatatype-e86a9f87","./WebGLConstants-fcb70ee3","./EllipseGeometryLibrary-d955e650","./GeometryAttribute-dacddb3f","./GeometryAttributes-899f8bd0","./GeometryInstance-1aacc2fb","./GeometryOffsetAttribute-d3a42805","./GeometryPipeline-7cd8f832","./AttributeCompression-5b18be52","./EncodedCartesian3-bf4e5ec3","./IndexDatatype-3a8ea78f","./IntersectionTests-f3382f21","./Plane-5bea24eb","./VertexFormat-7d5b4d7e"],(function(e,t,a,r,n,i,o,d,s,b,c,l,f,m,p,u,y,G,E,C,x,A){"use strict";return function(r,n){return t.defined(n)&&(r=a.EllipseGeometry.unpack(r,n)),r._center=e.Cartesian3.clone(r._center),r._ellipsoid=e.Ellipsoid.clone(r._ellipsoid),a.EllipseGeometry.createGeometry(r)}}));