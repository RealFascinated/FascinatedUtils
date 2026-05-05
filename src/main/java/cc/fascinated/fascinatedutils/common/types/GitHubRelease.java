package cc.fascinated.fascinatedutils.common.types;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class GitHubRelease {
    @SerializedName("tag_name")
    private String tagName;
    private String name;
    private List<GitHubAsset> assets;
}
