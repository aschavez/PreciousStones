package net.sacredlabyrinth.Phaed.PreciousStones.entries;

import net.sacredlabyrinth.Phaed.PreciousStones.*;
import net.sacredlabyrinth.Phaed.PreciousStones.vectors.Field;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;


public class FieldSign
{
    private Sign sign;
    private boolean valid = true;
    private String tag;
    private Field field;
    private boolean fieldSign;
    private int multiple;
    private String period;
    private int price;
    private boolean noEconomy;
    private BlockTypeEntry item;
    private String playerName;

    public FieldSign(Block block)
    {
        if (!SignHelper.isSign(block))
        {
            valid = false;
            return;
        }

        sign = ((Sign) block.getState());

        String[] lines = sign.getLines();

        valid = extractData(block, lines);
    }

    public FieldSign(Block block, String[] lines, Player player)
    {
        if (!SignHelper.isSign(block))
        {
            valid = false;
            return;
        }

        valid = extractData(block, lines);
        playerName = player.getName();
    }

    public boolean extractData(Block block, String[] lines)
    {
        tag = ChatColor.stripColor(lines[0]);

        price = SignHelper.extractPrice(ChatColor.stripColor(lines[1]));

        if (price == 0)
        {
            return false;
        }

        item = SignHelper.extractItemFromParenthesis(ChatColor.stripColor(lines[1]));

        if (item == null)
        {
            if (!PreciousStones.getInstance().getPermissionsManager().hasEconomy())
            {
                noEconomy = true;
                return false;
            }
        }

        if (!isBuyable())
        {
            period = ChatColor.stripColor(lines[2]);

            if (!SignHelper.isValidPeriod(period))
            {
                return false;
            }

            if (!SignHelper.isValidMultiple(ChatColor.stripColor(lines[3])))
            {
                return false;
            }

            multiple = SignHelper.multipleToInteger(ChatColor.stripColor(lines[3]));

            fieldSign = tag.equalsIgnoreCase(ChatBlock.format("fieldSignRent")) || tag.equalsIgnoreCase(ChatBlock.format("fieldSignBuy")) || tag.equalsIgnoreCase(ChatBlock.format("fieldSignShare"));

            if (!fieldSign)
            {
                return false;
            }
        }

        Block attachedBlock = SignHelper.getAttachedBlock(block);
        field = PreciousStones.getInstance().getForceFieldManager().getField(attachedBlock);

        if (field == null)
        {
            return false;
        }

        if (playerName != null)
        {
            if (!field.isOwner(playerName))
            {
                return false;
            }
        }

        if (isRentable())
        {
            if (!field.hasFlag(FieldFlag.RENTABLE))
            {
                return false;
            }
        }

        if (isShareable())
        {
            if (!field.hasFlag(FieldFlag.SHAREABLE))
            {
                return false;
            }
        }

        if (isBuyable())
        {
            if (!field.hasFlag(FieldFlag.BUYABLE))
            {
                return false;
            }
        }
        return true;
    }

    public boolean isValid()
    {
        return valid;
    }

    public boolean isRentable()
    {
        return tag.equalsIgnoreCase(ChatBlock.format("fieldSignRent"));
    }

    public boolean isBuyable()
    {
        return tag.equalsIgnoreCase(ChatBlock.format("fieldSignBuy"));
    }

    public boolean isShareable()
    {
        return tag.equalsIgnoreCase(ChatBlock.format("fieldSignShare"));
    }

    public void setRentedColor()
    {
        sign.setLine(0, ChatColor.RED + "" + ChatColor.BOLD + ChatColor.stripColor(sign.getLine(0)) + ChatColor.RED + "" + ChatColor.BOLD);
        sign.update();
    }

    public void setSharedColor()
    {
        sign.setLine(0, ChatColor.GOLD + "" + ChatColor.BOLD + ChatColor.stripColor(sign.getLine(0)) + ChatColor.RED + "" + ChatColor.BOLD);
        sign.update();
    }

    public void setNotRentedColor()
    {
        sign.setLine(0, ChatColor.BOLD + ChatColor.stripColor(sign.getLine(0)) + ChatColor.RED + "" + ChatColor.BOLD);
        sign.update();
    }

    /**
     * Throws the fieldSign back at the player
     */
    public void reject()
    {
        Helper.dropBlockWipe(sign.getBlock());
    }

    /**
     * Strips the block of its sign
     */
    public void strip()
    {
        FieldSign s = SignHelper.getAttachedFieldSign(field.getBlock());

        if (s != null)
        {
            Helper.dropBlockWipe(s.getSign().getBlock());
        }
    }

    /**
     * Returns the block the fieldSign is attacked to
     *
     * @return
     */
    public Block getAttachedBlock()
    {
        return SignHelper.getAttachedBlock(sign.getBlock());
    }

    public Field getField()
    {
        return field;
    }

    public boolean isFieldSign()
    {
        return fieldSign;
    }

    public String getPeriod()
    {
        return period;
    }

    public int getPrice()
    {
        return price;
    }

    public int getMultiple()
    {
        return multiple;
    }

    public BlockTypeEntry getItem()
    {
        return item;
    }

    public Sign getSign()
    {
        return sign;
    }

    public boolean isNoEconomy()
    {
        return noEconomy;
    }
}