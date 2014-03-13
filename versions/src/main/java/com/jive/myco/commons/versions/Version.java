package com.jive.myco.commons.versions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;

/**
 * @author Brandon Pedersen &lt;bpedersen@getjive.com&gt;
 */
@AllArgsConstructor
@EqualsAndHashCode
@Getter
public class Version implements Comparable<Version>
{
  public static final Pattern VERSION_FORMAT =
      Pattern.compile(
          "(?<major>\\d+)(?:\\.(?<minor>\\d+)(?:\\.(?<patch>\\d+))?)?(?:-(?<qualifier>\\w+))?");

  private final int major;
  private final int minor;
  private final int patch;
  private final String qualifier;

  public Version(int major, int minor, int patch)
  {
    this(major, minor, patch, null);
  }

  public static Version parseVersion(@NonNull String versionString) throws IllegalArgumentException
  {
    Matcher matcher = VERSION_FORMAT.matcher(versionString);
    Preconditions.checkArgument(matcher.matches(),
        "Version string [%s] is not valid, should be of the form major.minor.patch(-qualifier)",
        versionString);

    return new Version(
        Integer.parseInt(matcher.group("major")),
        Integer.parseInt(Optional.fromNullable(matcher.group("minor")).or("0")),
        Integer.parseInt(Optional.fromNullable(matcher.group("patch")).or("0")),
        matcher.group("qualifier"));
  }

  @Override
  public String toString()
  {
    if (qualifier == null || qualifier.isEmpty())
    {
      return String.format("%s.%s.%s", major, minor, patch);
    }
    else
    {
      return String.format("%s.%s.%s-%s", major, minor, patch, qualifier);
    }
  }

  @Override
  public int compareTo(Version other)
  {
    if (this == other)
    {
      return 0;
    }

    if (other == null)
    {
      return 1;
    }

    return ComparisonChain.start()
        .compare(this.major, other.major)
        .compare(this.minor, other.minor)
        .compare(this.patch, other.patch)
        .result();
  }
}
