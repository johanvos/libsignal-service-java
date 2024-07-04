package org.whispersystems.signalservice.api.messages;


import com.google.protobuf.MessageLite;
import java.util.List;
import java.util.Optional;
import org.whispersystems.signalservice.internal.push.SignalServiceProtos;
import org.whispersystems.signalservice.internal.push.SignalServiceProtos.TextAttachment;

public class SignalServiceTextAttachment {

  private final Optional<String>  text;
  private final Optional<Style>   style;
  private final Optional<Integer> textForegroundColor;
  private final Optional<Integer>              textBackgroundColor;
  private final Optional<SignalServicePreview> preview;
  private final Optional<Gradient>             backgroundGradient;
  private final Optional<Integer>              backgroundColor;

  private SignalServiceTextAttachment(Optional<String>               text,
                                      Optional<Style>                style,
                                      Optional<Integer>              textForegroundColor,
                                      Optional<Integer>              textBackgroundColor,
                                      Optional<SignalServicePreview> preview,
                                      Optional<Gradient>             backgroundGradient,
                                      Optional<Integer>              backgroundColor) {
    this.text                = text;
    this.style               = style;
    this.textForegroundColor = textForegroundColor;
    this.textBackgroundColor = textBackgroundColor;
    this.preview             = preview;
    this.backgroundGradient  = backgroundGradient;
    this.backgroundColor     = backgroundColor;
  }

  public static SignalServiceTextAttachment forGradientBackground(Optional<String>               text,
                                                                  Optional<Style>                style,
                                                                  Optional<Integer>              textForegroundColor,
                                                                  Optional<Integer>              textBackgroundColor,
                                                                  Optional<SignalServicePreview> preview,
                                                                  Gradient                       backgroundGradient) {
    return new SignalServiceTextAttachment(text,
                                           style,
                                           textForegroundColor,
                                           textBackgroundColor,
                                           preview,
                                           Optional.of(backgroundGradient),
                                           Optional.empty());
  }

  public static SignalServiceTextAttachment forSolidBackground(Optional<String>               text,
                                                               Optional<Style>                style,
                                                               Optional<Integer>              textForegroundColor,
                                                               Optional<Integer>              textBackgroundColor,
                                                               Optional<SignalServicePreview> preview,
                                                               int                            backgroundColor) {
    return new SignalServiceTextAttachment(text,
                                           style,
                                           textForegroundColor,
                                           textBackgroundColor,
                                           preview,
                                           Optional.empty(),
                                           Optional.of(backgroundColor));
  }

  public Optional<String> getText() {
    return text;
  }

  public Optional<Style> getStyle() {
    return style;
  }

  public Optional<Integer> getTextForegroundColor() {
    return textForegroundColor;
  }

  public Optional<Integer> getTextBackgroundColor() {
    return textBackgroundColor;
  }

  public Optional<SignalServicePreview> getPreview() {
    return preview;
  }

  public Optional<Gradient> getBackgroundGradient() {
    return backgroundGradient;
  }

  public Optional<Integer> getBackgroundColor() {
    return backgroundColor;
  }

    public TextAttachment.Builder toTextAttachmentBuilder() {
        TextAttachment.Builder builder = TextAttachment.newBuilder();

        if (getStyle().isPresent()) {
            switch (getStyle().get()) {
                case DEFAULT:
                    builder.setTextStyle(TextAttachment.Style.DEFAULT);
                    break;
                case REGULAR:
                    builder.setTextStyle(TextAttachment.Style.REGULAR);
                    break;
                case BOLD:
                    builder.setTextStyle(TextAttachment.Style.BOLD);
                    break;
                case SERIF:
                    builder.setTextStyle(TextAttachment.Style.SERIF);
                    break;
                case SCRIPT:
                    builder.setTextStyle(TextAttachment.Style.SCRIPT);
                    break;
                case CONDENSED:
                    builder.setTextStyle(TextAttachment.Style.CONDENSED);
                    break;
                default:
                    throw new AssertionError("Unknown type: " + getStyle().get());
            }
        }

        TextAttachment.Gradient.Builder gradientBuilder = TextAttachment.Gradient.newBuilder();

        if (getBackgroundGradient().isPresent()) {
            SignalServiceTextAttachment.Gradient gradient = getBackgroundGradient().get();

            if (gradient.getAngle().isPresent()) {
                gradientBuilder.setAngle(gradient.getAngle().get());
            }

            if (!gradient.getColors().isEmpty()) {
                gradientBuilder.setStartColor(gradient.getColors().get(0));
                gradientBuilder.setEndColor(gradient.getColors().get(gradient.getColors().size() - 1));
            }

            gradientBuilder.addAllColors(gradient.getColors());
            gradientBuilder.addAllPositions(gradient.getPositions());

            builder.setGradient(gradientBuilder.build());
        }

        if (getText().isPresent()) {
            builder.setText(getText().get());
        }
        if (getTextForegroundColor().isPresent()) {
            builder.setTextForegroundColor(getTextForegroundColor().get());
        }
        if (getTextBackgroundColor().isPresent()) {
            builder.setTextBackgroundColor(getTextBackgroundColor().get());
        }
        if (getBackgroundColor().isPresent()) {
            builder.setColor(getBackgroundColor().get());
        }
//        if (getPreview().isPresent()) {
//            builder.setPreview(createPreview(getPreview().get()));
//        }
        return builder;
    }


  public static class Gradient {
    private final Optional<Integer> angle;
    private final List<Integer>     colors;
    private final List<Float>       positions;

    public Gradient(Optional<Integer> angle, List<Integer> colors, List<Float> positions) {
      this.angle      = angle;
      this.colors     = colors;
      this.positions  = positions;
    }

    public Optional<Integer> getAngle() {
      return angle;
    }

    public List<Integer> getColors() {
      return colors;
    }

    public List<Float> getPositions() {
      return positions;
    }
  }

  public enum Style {
    DEFAULT,
    REGULAR,
    BOLD,
    SERIF,
    SCRIPT,
    CONDENSED,
  }
}
