#version 330

layout (std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

float sdRoundedRectCornersYDown(vec2 pointPx, vec2 halfSizePx, vec4 rCorners) {
    vec2 rSide = (pointPx.x > 0.0) ? vec2(rCorners.g, rCorners.b) : vec2(rCorners.r, rCorners.a);
    float rad = (pointPx.y < 0.0) ? rSide.x : rSide.y;
    rad = min(rad, min(halfSizePx.x, halfSizePx.y) - 1e-4);
    vec2 q = abs(pointPx) - halfSizePx + vec2(rad);
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - rad;
}

void main() {
    vec4 texSample = texture(Sampler0, texCoord0);
    vec2 uv = texCoord0;
    vec2 h = max(fwidth(uv), vec2(1e-5));
    float widthPx = 1.0 / h.x;
    float heightPx = 1.0 / h.y;
    vec2 halfSizePx = vec2(widthPx, heightPx) * 0.5;
    vec2 pointPx = (uv - vec2(0.5)) * vec2(widthPx, heightPx);
    float minSidePx = min(widthPx, heightPx);
    float rPx = clamp(vertexColor.a * 255.0, 0.0, minSidePx * 0.5);
    vec4 rCorners = vec4(rPx, rPx, rPx, rPx);
    float signedDistance = sdRoundedRectCornersYDown(pointPx, halfSizePx, rCorners);
    float edgeWidth = max(fwidth(signedDistance), 1e-4);
    float coverage = 1.0 - smoothstep(-edgeWidth, edgeWidth, signedDistance);
    if (coverage <= 0.001) {
        discard;
    }
    vec3 tintRgb = vertexColor.rgb;
    float outAlpha = texSample.a * coverage * ColorModulator.a;
    fragColor = vec4(tintRgb * ColorModulator.rgb, outAlpha);
}
