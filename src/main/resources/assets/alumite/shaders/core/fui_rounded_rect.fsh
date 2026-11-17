#version 330

// Can't moj_import in things used during startup, when resource packs don't exist.
// This is a copy of dynamictransforms.glsl
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

#ifdef TEXTURED
uniform sampler2D Sampler0;
#endif

in vec2 localPos;
in vec4 vertexColor;
flat in vec2 halfSize;
flat in vec4 cornerRadii;
flat in float ringStroke;
in vec2 texCoord;

out vec4 fragColor;

// Signed distance to a rectangle with independent corner radii (top left, top right, bottom right, bottom left);
// GUI y is down, so the top is negative y from the centre.
float sdRoundedBox(vec2 point, vec2 halfExtent, vec4 radii) {
    vec2 side = (point.x > 0.0) ? vec2(radii.y, radii.z) : vec2(radii.x, radii.w);
    float radius = (point.y < 0.0) ? side.x : side.y;
    radius = min(radius, min(halfExtent.x, halfExtent.y));
    vec2 corner = abs(point) - halfExtent + vec2(radius);
    return min(max(corner.x, corner.y), 0.0) + length(max(corner, 0.0)) - radius;
}

void main() {
    float distance = sdRoundedBox(localPos, halfSize, cornerRadii);
    // Distance is in local units, so the gradient length is local units per framebuffer pixel.
    float edge = max(length(vec2(dFdx(distance), dFdy(distance))), 1e-5);
    float coverage = clamp(0.5 - distance / edge, 0.0, 1.0);
    if (ringStroke > 0.0) {
        float insideInner = clamp(0.5 - (distance + ringStroke) / edge, 0.0, 1.0);
        coverage *= 1.0 - insideInner;
    }
    if (coverage <= 0.001) {
        discard;
    }
    vec4 color = vertexColor;
#ifdef TEXTURED
    color *= texture(Sampler0, texCoord);
#endif
    fragColor = vec4(color.rgb * ColorModulator.rgb, color.a * coverage * ColorModulator.a);
}
