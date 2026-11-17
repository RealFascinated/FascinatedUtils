#version 330

// Can't moj_import in things used during startup, when resource packs don't exist.
// This is a copy of dynamictransforms.glsl and projection.glsl
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};
layout(std140) uniform Projection {
    mat4 ProjMat;
};

// Position.xy is the screen position; Position.z carries the corner radius in local units (z is forced to 0).
in vec3 Position;
// Local offset from the shape centre in local units (not a texture coordinate).
in vec2 UV0;
in vec4 Color;
// Shape half size in 1/8 local unit steps.
in ivec2 UV1;
// x: corner mask (1 top left, 2 top right, 4 bottom left, 8 bottom right), y: ring stroke in 1/64 local unit steps.
// With TEXTURED the shape is always a solid fill with all corners rounded, and UV2 is instead the texture coordinate
// of the vertex in 1/16384 steps.
in ivec2 UV2;

out vec2 localPos;
out vec4 vertexColor;
flat out vec2 halfSize;
flat out vec4 cornerRadii;
flat out float ringStroke;
out vec2 texCoord;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position.xy, 0.0, 1.0);

    localPos = UV0;
    vertexColor = Color;
    halfSize = vec2(UV1) * 0.125;
#ifdef TEXTURED
    ringStroke = 0.0;
    texCoord = vec2(UV2) * (1.0 / 16384.0);
    int mask = 15;
#else
    ringStroke = float(UV2.y) * (1.0 / 64.0);
    texCoord = vec2(0.0);
    int mask = UV2.x;
#endif
    float radius = Position.z;
    // Order matches the shader SDF: top left, top right, bottom right, bottom left.
    cornerRadii = vec4(
        (mask & 1) != 0 ? radius : 0.0,
        (mask & 2) != 0 ? radius : 0.0,
        (mask & 8) != 0 ? radius : 0.0,
        (mask & 4) != 0 ? radius : 0.0
    );
}
