package com.jive.myco.commons.versions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.NonNull;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

/**
 * @author Brandon Pedersen &lt;bpedersen@getjive.com&gt;
 */
public class VersionRange
{
  private static final Pattern OPTIONAL_VERSION =
      Pattern.compile("^(?:" + Version.VERSION_FORMAT + ")?$");

  private final InclusionType minType;
  private final Version minVersion;
  private final InclusionType maxType;
  private final Version maxVersion;

  public VersionRange(String range)
  {
    String errorMessage = "Range '" + range + "' is not valid";
    Preconditions.checkArgument(range != null && !range.isEmpty(), errorMessage);

    Matcher matcher = Version.VERSION_FORMAT.matcher(range);
    if (matcher.matches())
    {
      minVersion = maxVersion = Version.parseVersion(range);
      minType = maxType = InclusionType.INCLUSIVE;
    }
    else
    {
      Preconditions.checkArgument(range.length() >= 3, errorMessage);

      minType = InclusionType.fromString("" + range.charAt(0));
      maxType = InclusionType.fromString("" + range.charAt(range.length() - 1));

      String[] versions = range.substring(1, range.length() - 1).split(",", -1);

      Preconditions.checkArgument(versions.length == 2, errorMessage);

      matcher = OPTIONAL_VERSION.matcher(versions[0]);
      if (!matcher.matches())
      {
        throw new IllegalArgumentException(errorMessage);
      }

      String major = matcher.group("major");
      String minor = matcher.group("minor");
      String patch = matcher.group("patch");

      if (major == null)
      {
        minVersion = null;
      }
      else
      {
        minVersion = new Version(
            Integer.parseInt(major),
            Integer.parseInt(Optional.fromNullable(minor).or("0")),
            Integer.parseInt(Optional.fromNullable(patch).or("0")));
      }


      matcher = OPTIONAL_VERSION.matcher(versions[1]);
      if (!matcher.matches())
      {
        throw new IllegalArgumentException(errorMessage);
      }

      major = matcher.group("major");
      minor = matcher.group("minor");
      patch = matcher.group("patch");

      if (major == null)
      {
        maxVersion = null;
      }
      else
      {
        maxVersion = new Version(
            Integer.parseInt(major),
            Integer.parseInt(Optional.fromNullable(minor).or("0")),
            Integer.parseInt(Optional.fromNullable(patch).or("0")));
      }
    }
  }

  public boolean isInRange(String version)
  {
    return isInRange(Version.parseVersion(version));
  }

  public boolean isInRange(@NonNull Version version)
  {
    // Validate above min level
    if (minVersion != null)
    {
      int compare = minVersion.compareTo(version);
      if (compare > 0 || (compare == 0 && minType == InclusionType.EXCLUSIVE))
      {
        return false;
      }
    }

    // Validate below max level
    if (maxVersion != null)
    {
      int compare = maxVersion.compareTo(version);
      if (compare < 0 || (compare == 0 && maxType == InclusionType.EXCLUSIVE))
      {
        return false;
      }
    }

    return true;
  }

  public static enum InclusionType
  {
    EXCLUSIVE("(", ")"),
    INCLUSIVE("[", "]");


    private final String leftBrace;
    private final String rightBrace;

    InclusionType(String leftBrace, String rightBrace)
    {
      this.leftBrace = leftBrace;
      this.rightBrace = rightBrace;
    }

    public static InclusionType fromString(String text)
    {
      for (InclusionType type : values())
      {
        if (type.leftBrace.equals(text) || type.rightBrace.equals(text))
        {
          return type;
        }
      }

      throw new IllegalArgumentException("Invalid inclusion type '" + text + "'.");
    }
  }


}
