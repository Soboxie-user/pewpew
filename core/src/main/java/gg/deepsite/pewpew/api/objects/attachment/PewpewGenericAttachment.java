package gg.deepsite.pewpew.api.objects.attachment;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * An attachment for a slot type registered from {@code attachment-menu.yml}. It has no fields of its own;
 * everything it does comes from the {@code modifiers} block on {@link PewpewAttachment}.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PewpewGenericAttachment extends PewpewAttachment {
}
