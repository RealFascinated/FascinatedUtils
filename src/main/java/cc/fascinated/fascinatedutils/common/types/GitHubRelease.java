package cc.fascinated.fascinatedutils.common.types;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GitHubRelease {
    private String tagName;
    private String name;
    private List<GitHubAsset> assets;
}
