package com.iseria.domain;

import com.iseria.ui.UIHelpers;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static com.iseria.domain.DATABASE.AuxBuilding.*;
import static com.iseria.domain.DATABASE.MainBuilding.*;
import static com.iseria.domain.DATABASE.ResourceType.*;


public class DATABASE {

    ///1 SdN = 10 Nourritures pour la Population, 1 Plat Cuisiné = 1.25
    public enum MoralAction {
        AUGMENTATION_SALAIRES(
                "Augmentation des Salaires",
                "Payez 10% de plus de salaire cette semaine. Réduisez de 10% l'Instabilité. Gagnez un point de Moral",
                "Vous augmentez le Personnel de votre Maisonnée ",
                1, -10, ActionType.REPETABLE, false, 0, null
        ),
        AUGMENTATION_TAXES(
                "Augmentation des Taxes",
                "Payez 10% de moins sur votre salaire. Augmentez votre Instabilité de 10%. Perdez deux points de Moral",
                "Vous augmentez les Taxes !",
                -2, 10, ActionType.REPETABLE, false, 0, null
        ),
        AVARICE(
                "Avarice",
                "Perdez 1 point de Moral. Réduisez l’Instabilité de 10%, Ratio de 15%{SalaireArmée} = 1 Moral",
                "Maisonnée Marchande uniquement  Vous dépenser de l’Or pour augmenter le Moral d’armée.",
                +1, -10, ActionType.REPETABLE, false, 0, "Merchantile"
        ),
        BASSESSE(
                "Bassesse",
                "Perdez deux points de Moral. Augmentez votre Instabilité de 40%",
                "Vous avez fait preuve (publiquement) de bassesse.\n" +
                        "De part la découverte d’un espion vous appartenant, d’une tentative d’assassinat raté ou d’un complot avorté …",
                -2, 40, ActionType.AUTO, false, 0, null
        ),
        WITCH_HUNT(
                " Chasse au sorcière",
                "Gagnez un point de Moral\n" +
                        "Perdez 10 de Diplomatie avec les Pouvoir séculaires Loyaux Bons. Réduisez votre Instabilité de 25%",
                "Vous définissez un bouc émissaire pour votre Personnel. ",
                +1, -25, ActionType.UNIQUE, false, 0, null
        ),
        WRONGWAY(
                "Direction Incohérente",
                "Perdez un point de Moral, augmentez votre Instabilité de 10 %",
                "Vous agissez de manière contraire à votre alignement et votre Personnel le désapprouve ",
                -1, 10, ActionType.AUTO, false, 0, null
        ),
        FESTIVAL(
                "Festival!",
                "Vous gagnez 1 point de Moral par tranche de 500 po investis, augmentez votre Instabilité de 10%.",
                "Vous organisez un Festival ! ",
                +1, 10, ActionType.UNIQUE, false, 0, null
        ),
        OOPSIES(
                "Indiscrétion",
                "Perdez un point de Moral, augmentez votre Instabilité de 10%",
                "Vous avez laissée passer des informations sensibles",
                -1, 10, ActionType.AUTO, false, 0, null
        ),
        BARABAS(
                "Libération",
                "Gagnez un point de Moral mais perdez un point de Moral d’Armée, augmentez votre Instabilité de 15%",
                "Vous laissez votre Personnel choisir un individu criminel à libérer.",
                +1, 15, ActionType.REPETABLE, false, 0, null
        ),
        LAYOFF(
                "Licenciement",
                "Perdez un Points de Moral",
                "Vous remerciez jusqu’à 100 individus travaillant pour vous.",
                -1, 0, ActionType.REPETABLE, false, 0, null
        ),
        LOI_MARTIALE(
                "Loi Martiale",
                "Gagnez deux points de Moral mais votre moral de base passe à 4 tant qu'elle est active",
                "Vous établissez la Loi Martiale.",
                +2, -10, ActionType.UNIQUE, true, -1, null
        ),
        PACTE(
                "Pacte",
                "Gagnez un point de Moral,  réduisez votre Instabilité de 10%",
                "Vous avez établi un contrat public de Maisonnée, un accord entre joueurs ou avec un Pouvoir Séculaire, ou vous avez simplement accompli votre parole",
                +1, -10, ActionType.AUTO, false, 0, null
        ),
        RUMORBAD(
                "Râgôts Négatifs",
                "Perdez un point de Moral, augmentez votre Instabilité de 10%",
                "Vous êtes sujets à de nombreuses rumeurs qui se prêtent à devenir réalité dans l’inconscient collectif. ",
                -1, 10, ActionType.AUTO, false, 0, null
        ),
        RUMORGOOD(
                "Ragôts Positifs",
                "Gagnez un point de Moral, réduisez votre Instabilité de 10%",
                "Vous êtes sujets à de nombreuses rumeurs qui se prêtent à devenir réalité dans l’inconscient collectif.",
                +1, -10, ActionType.AUTO, false, 0, null
        ),
        JOESLOWDOWN(
                "Relâchement de la cadence",
                "Gagnez deux points de Moral, réduisez votre instabilité de 30%",
                "Vous diminuez de 50% la cadence de vos Producteurs.",
                +2, -30, ActionType.UNIQUE, false, 0, null
        ),
        JOEFASTER(
                "Travail Forcé",
                "Perdez trois points de Moral, augmentez votre Instabilité de 25%",
                "Vous augmentez de 50% la cadence de vos Producteurs. ",
                -2, 25, ActionType.UNIQUE, false, 0, null
        ),
        LONGLIVETHEKING(
                "Trahison !",
                "Perdez deux points de Moral\n" +
                        "Perte 50 Diplomatie avec les Pouvoir séculaires Loyaux, augmentez votre Instabilité de 20%",
                "Vous brisez un contrat public de Maisonnée un accord entre vous et un Pouvoir Séculaire ou vous manquez simplement à votre parole ",
                -2, 20, ActionType.AUTO, false, 0, null
        ),
        CALLMESTALIN(
                "Tyrannie",
                "Votre Moral de Base devient 3 mais :\n" +
                        "Les bénéfices accordés par des actions négatives sont augmentés de 50%.\n" +
                        "Maintiens l’Instabilité à 0 pendant 5 semaines, puis +10% par semaine.",
                "Vous régnez avec Tyrannie. ",
                0, 0, ActionType.UNIQUE, true, -2, null

        ),
        ;


        private final String name;
        private final String description;
        private final String loreDescription;
        private final int moralEffect;
        private final int instabilityEffect;
        private final ActionType type;
        private final boolean permanent;
        private final int baseMoralModifier;
        private final String requiredFactionType;

        MoralAction(String name, String description, String loreDescription, int moralEffect,
                    int instabilityEffect, ActionType type, boolean permanent, int baseMoralModifier, String requiredFactionType) {
            this.name = name;
            this.description = description;
            this.loreDescription = loreDescription;
            this.moralEffect = moralEffect;
            this.instabilityEffect = instabilityEffect;
            this.type = type;
            this.permanent = permanent;
            this.baseMoralModifier = baseMoralModifier;
            this.requiredFactionType = requiredFactionType;

        }


        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getLoreDescription() { return loreDescription; }
        public int getMoralEffect() { return moralEffect; }
        public int getInstabilityEffect() { return instabilityEffect; }
        public ActionType getType() { return type; }
        public int getBaseMoralModifier() { return baseMoralModifier; }
        public String getRequiredFactionType() { return requiredFactionType; }

        public enum ActionType {
            UNIQUE, REPETABLE, AUTO
        }

        // Vérification des restrictions de faction
        public boolean isAvailableForFaction(Faction faction) {
            return requiredFactionType == null ||
                    requiredFactionType.equals(faction.getFactionType());
        }



        // Méthode utilitaire pour obtenir les actions disponibles pour une faction
        public static List<MoralAction> getAvailableActionsFor(Faction faction) {
            return Arrays.stream(values())
                    .filter(action -> action.isAvailableForFaction(faction))
                    .collect(Collectors.toList());
        }
    }

    public sealed interface JobBuilding permits MainBuilding, AuxBuilding, FortBuilding {
        String getTag();
        String getBuildName();
    }

    public enum MainBuilding  implements JobBuilding {
            MAIN_00("Main0", "Free Slot"),

            MAIN_1_0("Main1", "Donjon"),


            MAIN_2_0("Main2","Abbaye"),
            MAIN_2_1("Main3", "Abbaye Tier 1", 30,0,20,15,0,0,1),

            MAIN_3_0("Main4","Carrière - Argile"),
            MAIN_3_1("Main5", "Carrière - Argile Tier 1", 45,0,5,10, 0,0,1),

            MAIN_4_0("Main6","Carrière - Pierre"),
            MAIN_4_1("Main7", "Carrière - Pierre Tier 1", 45,0,5,10,0,0,1),
            MAIN_4_2("Main8","Carrière - Pierre Tier 2", 10,0,100,50,25,0,2),

            MAIN_5_0("Main9","Commanderie"),
            MAIN_5_1("Main10", "Commanderie Tier 1", 25,0,25,10,0,0,1  ),

            MAIN_6_0("Main11","Avant-Poste", 10,0,10,0,0,500,1),

            MAIN_7_0("Main12", "Couvent"),
            MAIN_7_1("Main13", "Couvent Tier 1", 25,0,15,10,0,0,1),

            MAIN_8_0("Main14", "Scierie"),
            MAIN_8_1("Main15","Scierie Tier 1", 45,0,5,10,0,0,1),

            MAIN_9_0("Main16", "Tenturerie"),
            MAIN_9_1("Main17", "Tenturerie Tier 1",15,0,35,10,0,0,1),

            MAIN_10_0("Main18", "Entrepôt",40,0,40,0,0,0,0),
            MAIN_10_1("Main19", "Entrepôt Tier 1",60,0,60,0,0,0,1),
            MAIN_10_2("Main20", "Entrepôt Tier 2",100,0,100,0,0,0,2),

            MAIN_11_0("Main21","Ferme de Légume"),
            MAIN_11_1("Main22", "Ferme de Légume Tier 1", 45,0,5,10,0,0,1),
            MAIN_11_2("Main23", "Ferme de Légume Tier 2",10,0,100,50,25,0,2),

            MAIN_12_0("Main24","Mine"),
            MAIN_12_1("Main25","Mine Tier 1", 45,5,0,10,0,0,1),
            MAIN_12_2("Main26","Mine Tier 2", 25,15,50,50,25,0,2),

            MAIN_13_0("Main27","Oratoire",0,0,0,0,0,0,0),
            MAIN_13_1("Main28","Oratoire Tier 1",25,0,0,25,0,0,1, EnumSet.of(SpecialResource.REG_ARTWORK)),

            Main_14_0("Main29", "Thermes"),
            MAIN_14_1("Main30","Thermes Tier 1", 20,0,10,5,0,0,1),

            MAIN_15_0("Main31", "Atelier de Marbrier"),
            MAIN_15_1("Main32", "Atelier de Marbrier Tier 1",10,0,40,10,0,0,1 ),

            MAIN_16_0("Main33", "Observatoire"),
            MAIN_16_1("Main34", "Observatoire Tier 1",30,0,5,10,0,0,1),

            MAIN_17_0("Main35", "Saliculture"),
            MAIN_17_1("Main36", "Saliculture Tier 1", 20,0,15,5,0,0,1),

            MAIN_18_0("Main37", "Corderie"),
            MAIN_18_1("Main38", "Corderie Tier 1", 15,0,35,10,0,0,1),

            MAIN_19_0("Main39", "Atelier de Soufflage de Verre"),
            MAIN_19_1("Main40", "Atelier de Soufflage de Verre Tier 1", 15,0,35,10,0,0,1),

            MAIN_20_0("Main41", "Torréfacteur"),
            MAIN_20_1("Main42", "Torréfacteur Tier 1", 15,0,35,10,0,0,1),

            MAIN_21_0("Main43", "Four à Goudron"),
            MAIN_21_1("Main44","Four à Goudron Tier 1", 40,0,5,5,0,0,1),

            MAIN_22_0("Main45", "Tréfilerie"),
            MAIN_22_1("Main46", "Tréfilerie Tier 1", 15,0,35,10,0,0,1),

            MAIN_23_0("Main47", "Pépinière"),
            MAIN_23_1("Main48", "Pépinière Tier 1", 15,15,0,15,0,0,0,20),

            MAIN_24_0("Main49", "Vergers"),
            MAIN_24_1("Main50", "Vergers Tier 1", 20,0,10,20,0,0,1),

            MAIN_25_0("Main51", "Place-Forte"),

            MAIN_26_0("Main52", "Camp de Bûcheron"),

            MAIN_27_0("Main53", "Camp de Pêcheurs"),

            MAIN_28_0("Main54", "Ferme de Céréale"),
            MAIN_28_1("Main55", "Ferme de Céréale Tier 1", 45,0,5,10,0,0,1),
            MAIN_28_2("Main56", "Ferme de Céréale Tier 2",10,0,100,50,25,0,2),

            MAIN_29_0("Main57","Pâturages"),
            MAIN_30_0("Main58", "Camp de chasseurs"),

            MAIN_1111("Main01", "Place-Forte");


            @Override
            public String getTag() { return tag; }
            @Override
            public String getBuildName() {return name;}
            public String getMainTag() { return tag; }
            public int getMainCost() { return costPierre; }
            public int getMainTier() { return tier; }



            private final String tag;
            private final String name;
            private final int costPierre;
            private final int costFer;
            private final int costBois;
            private final int costPop;
            private final int costGemWeekly;
            private final int costGold;
            private final int tier;
            private int costGlass;
            private EnumSet<SpecialResource> specialResources;


            MainBuilding(String tag, String name, int costPierre, int costFer,  int costBois, int costPop,int costGemWeekly,
                         int costGold, int tier){
                this.tag = tag;
                this.name = name;
                this.costPierre = costPierre;
                this.costFer= costFer;
                this.costBois = costBois;
                this.costPop= costPop;
                this.costGemWeekly = costGemWeekly;
                this.costGold = costGold;
                this.tier = tier;}

            MainBuilding(String tag, String name, int costPierre, int costFer, int costBois, int costPop, int costGemWeekly,
                         int costGold, int tier, EnumSet<SpecialResource> specialResources) {
                this.tag = tag;
                this.name = name;
                this.costPierre = costPierre;
                this.costFer = costFer;
                this.costBois = costBois;
                this.costPop = costPop;
                this.costGemWeekly = costGemWeekly;
                this.costGold = costGold;
                this.tier = tier;
                this.specialResources = specialResources;
            }

            MainBuilding(String tag, String name, int costPierre, int costFer, int costBois, int costPop, int costGemWeekly,
                         int costGold, int tier, int costGlass) {
                this.tag = tag;
                this.name = name;
                this.costPierre = costPierre;
                this.costFer = costFer;
                this.costBois = costBois;
                this.costPop = costPop;
                this.costGemWeekly = costGemWeekly;
                this.costGold = costGold;
                this.tier = tier;
                this.costGlass = costGlass;
            }

            MainBuilding(String tag, String name) {
                this(tag, name, 0, 0, 0, 0, 0, 0, 0);
            }



            @Override
            public String toString() {
                return tag;
            }
        }

    public enum AuxBuilding  implements JobBuilding {
            AUX_0("Aux 0", "Free Slots"),
            AUX_1_0("Aux 1", "Moulin"),
            AUX_1_1("Aux 2","Moulin tier 1",5,0,45,10,0,0,1),
            AUX_1_2("Aux 3","Moulin tier 2", 10,0,100,50,25,0,2),
            AUX_2_0("Aux 4","Moulin à eau"),
            AUX_2_1("Aux 5","Moulin à eau tier 1",5,0,45,10,0,0,1),
            AUX_2_2("Aux 6","Moulin à eau tier 2", 10,0,100,50,25,0,2),
            AUX_3_0("Aux 7", "Cloître"),
            AUX_4_0("Aux 8","Prieuré"),
            AUX_5_0("Aux 9", "Fonderie"),
            AUX_5_1("Aux 10", "Fonderie tier 1", 40,10,0,0,0,0,1),
            AUX_6_0("Aux 11", "Atelier d'Artisan"),
            AUX_7_0("Aux 12", "Cuisines"),
            AUX_8_0("Aux 13", "Atelier de Maçonnerie"),
            AUX_8_1("Aux 14", "Atelier de Maçonnerie tier 1", 20,0,20,100,0,0,1),
            AUX_9_0("Aux 15","Chapelle de Campagne"),
            AUX_9_1("Aux 16", "Eglise", 25,0,25,10,0,0,1),
            AUX_9_2("Aux 17", "Cathédrale", 100,0,100,50,0,0,2,20),
            AUX_10_0("Aux 18", "Serres de Culture"),
            AUX_10_1("Aux 19", "Serres de Culture tier 1", 15,0,25,5,0,0,1,10),
            AUX_11_0("Aux 20", "Tribunal"),
            AUX_12_0("Aux 21","Ruches et Atelier d'Apiculteur"),
            AUX_12_1("Aux 22","Ruches et Atelier d'Apiculteur tier 1", 0,0,0,5,0,0,1),
            AUX_13_0("Aux 23","Péage"),
            AUX_14_0("Aux 24","Cave d'affinage"),
            AUX_14_1("Aux 25", "Cave d'affinagetier 1", 10,0,25,15,0,0,1),
            AUX_15_0("Aux 26", " Poste Frontière"),
            AUX_16_0("Aux 27", "Pressoir"),
            AUX_16_1("Aux 28", "Pressoir tier 1", 15,0,35,10,0,0,1),
            AUX_17_0("Aux 29", "Atelier");


            private final String tag;
            private final String name;
            private final int costPierre;
            private final int costFer;
            private final int costBois;
            private final int costPop;
            private final int costGemWeekly;
            private final int costGold;
            private final int tier;
            private int costGlass;


            AuxBuilding(String tag, String name, int costPierre, int costFer,  int costBois, int costPop,int costGemWeekly,
                         int costGold, int tier){
                this.tag = tag;
                this.name = name;
                this.costPierre = costPierre;
                this.costFer= costFer;
                this.costBois = costBois;
                this.costPop= costPop;
                this.costGemWeekly = costGemWeekly;
                this.costGold = costGold;
                this.tier = tier;}
            AuxBuilding(String tag, String name) {
                this(tag, name, 0, 0, 0, 0, 0, 0, 0);}
            AuxBuilding(String tag, String name, int costPierre, int costFer, int costBois, int costPop, int costGemWeekly,
                         int costGold, int tier, int costGlass) {
                this.tag = tag;
                this.name = name;
                this.costPierre = costPierre;
                this.costFer = costFer;
                this.costBois = costBois;
                this.costPop = costPop;
                this.costGemWeekly = costGemWeekly;
                this.costGold = costGold;
                this.tier = tier;
                this.costGlass = costGlass;
            }
        @Override
            public String getTag() { return tag; }
            @Override public String getBuildName() {return name;}
            public String getAuxTag() { return tag; }
            public int getAuxCost() { return costPierre; }
            public int getAuxTier() { return tier; }
            @Override
            public String toString() {
                return tag;
            }
        }

    public enum FortBuilding  implements JobBuilding {

            FORT_0("Fort 0", "Free Slot"),
            FORT_1("Fort 1", "Pallisade", 0,0,10,0,0,0,0),
            FORT_2("Fort 2", "Murs",10,0,0,0,0,0,0),
            FORT_3("Fort 3", "Murailles", 20,0,0,0,0,0,0),
            FORT_4("Fort 4", "Douve", 0,0,0,10,0,0,0),
            FORT_5("Fort 5", "Tour de Guets", 5,0,5,0,0,250,0);


            private final String tag;
            private final String name;
            private final int costPierre;
            private final int costFer;
            private final int costBois;
            private final int costPop;
            private final int costGemWeekly;
            private final int costGold;
            private final int tier;
            private int costGlass;

            FortBuilding(String tag, String name, int costPierre, int costFer,  int costBois, int costPop,int costGemWeekly,
                        int costGold, int tier){
                this.tag = tag;
                this.name = name;
                this.costPierre = costPierre;
                this.costFer= costFer;
                this.costBois = costBois;
                this.costPop= costPop;
                this.costGemWeekly = costGemWeekly;
                this.costGold = costGold;
                this.tier = tier;}
            FortBuilding(String tag, String name) {
                this(tag, name, 0, 0, 0, 0, 0, 0, 0);}
            FortBuilding(String tag, String name, int costPierre, int costFer, int costBois, int costPop, int costGemWeekly,
                        int costGold, int tier, int costGlass) {
                this.tag = tag;
                this.name = name;
                this.costPierre = costPierre;
                this.costFer = costFer;
                this.costBois = costBois;
                this.costPop = costPop;
                this.costGemWeekly = costGemWeekly;
                this.costGold = costGold;
                this.tier = tier;
                this.costGlass = costGlass;
            }

            @Override public String getBuildName() {return name;}
            @Override public String getTag() { return tag; }
            public String getFortTag() { return tag; }
            public int getFortCost() { return costPierre; }
            public int getFortTier() { return tier; }
            @Override public String toString() { return tag;}
        }

    public enum SpecialResource {
            ARTIFACT, RELIC, BLUEPRINT, SCROLL, ARTWORK, REG_ARTWORK
        }

    public enum ResourceType {


        //ANIMAUX

        DROMADAIRES("Animaux","🐾","Dromadaires",  8.0, false,0),
        CHEVAUX("Animaux","🐾","Chevaux",  8.0, false,0),
        OURS("Animaux","🐾","Ours",  8.0, false,0),
        LEZARDS_GEANTS("Animaux","🐾","Lézards Géants",  8.0, false,0),
        ARAIGNEES_GEANTES("Animaux","🐾", "Araignées Géantes", 8.0, false,0),
        LIONS_DE_NEMEE("Animaux","🐾","Lions de Némée",  8.0, false,0),

        //ANIMAUX FERMIERS

        CANARD("Animaux FERMIERS","🐾","CANARD",  0.5, false,0, 180),
        CHEVRE("Animaux FERMIERS","🐾","CHEVRE",  3, false,0,100),
        MOUTON("Animaux FERMIERS","🐾","MOUTON",  4, false,0,90),
        PORC("Animaux FERMIERS","🐾","PORC",  5, false,0,65),
        POULE("Animaux FERMIERS","🐾","POULE",  0.25, false,0,450),
        VACHE("Animaux FERMIERS","🐾","VACHE",  8.0, false,0,270),


        //ARTISANAT

        MEUBLES("Artisanat", "🔧","meubles", 0,true, 0),
        BATEAUX("Artisanat", "🔧","bateaux", 0,true,0),
        ENGINS_DE_SIÈGE("Artisanat", "🔧","engins_de_siège",0,true,0),
        BÂTIMENTS("Artisanat", "🔧", "bâtiments", 0,true,0),
        VÊTEMENTS("Artisanat", "🔧", "vêtements", 0,true,0),
        PLATS_CUISINÉS("Artisanat", "🔧", "plats_cuisinés", 0,true,0),
        LIVRES("Artisanat", "🔧", "livres", 0,true,0),
        BIJOUX("Artisanat", "🔧", "bijoux", 0,true,0),
        OUTILS("Artisanat", "🔧", "outils", 0,true,0),
        PARCHEMINS("Artisanat", "🔧", "parchemins", 0,true,0),

        //NOURRITURE

        SAC_DE_NOURRITURE("Bloc Nourritures", "🔧", "sac_de_nourriture", 5.1,false,0),
        SAC_DE_NOURRITURE_TRAVAILLE("Bloc Nourritures", "🔧", "sac_de_nourriture_travaillé",0 ,true,10.3),
        PLAT_CUISINÉ("Bloc Nourritures", "🔧", "plat_cuisiné", 0,true,7.2),

        //CULTURES

        CHANVRE("Culture", "🔧", "chanvre", 0,false,0),
        COTON("Culture", "🔧", "coton", 0,false,0),
        FLEURS("Culture", "🔧", "fleurs", 0,false,0),
        HERBES_MÉDICINALES("Culture", "🔧", "herbes_médicinales", 0,false,0),

        //GEMMES&JOYAUX

        EMERAUDE("Gemmes", "💎", "emeraude", 0,false,0),
        PETIT_DIAMANT("Gemmes","💎" , "petit_diamant", 0,false,0),
        PETIT_RUBIS("Gemmes","💎" , "petit_rubis", 0,false,0),
        SAPPHIRE("Gemmes", "💎", "sapphire", 0,false,0),
        PETIT_DIAMANT_TAILLÉ("Gemmes Taillé","💎" , "petit_diamant_taillé", 0,true,0),
        PETIT_RUBIS_TAILLÉ("Gemmes Taillé","💎" , "petit_rubis_taillé", 0,true,0),
        SAPPHIRE_TAILLÉ("Gemmes Taillé","💎" , "sapphire_taillé", 0,true,0),
        EMERAUDE_TAILLÉ("Gemmes Taillé", "💎", "emeraude_taillé", 0,true,0),
        EMERAUDE_VERTE_BRILLANTE("Joyaux", "💎", "emeraude_verte_brillante", 0,false,0),
        GRAND_DIAMANT("Joyaux", "💎", "grand_diamant", 0,false,0),
        GRAND_RUBIS("Joyaux", "💎", "grand_rubis", 0,false,0),
        RUBIS_ÉTOILÉ("Joyaux", "💎", "rubis_étoilé", 0,false,0),
        SAPPHIRE_ÉTOILÉ("Joyaux", "💎", "sapphire_étoilé", 0,false,0),
        EMERAUDE_VERTE_BRILLANTE_TAILLÉ("Joyaux Taillé", "💎", "emeraude_verte_brillante_taillé", 0,true,0),
        GRAND_DIAMANT_TAILLÉ("Joyaux Taillé", "💎", "grand_diamant_taillé", 0,true,0),
        GRAND_RUBIS_TAILLÉ("Joyaux Taillé", "💎", "grand_rubis_taillé", 0,true,0),
        RUBIS_ÉTOILÉ_TAILLÉ("Joyaux Taillé", "💎", "rubis_étoilé_taillé", 0,true,0),
        SAPPHIRE_ÉTOILÉ_TAILLÉ("Joyaux Taillé", "💎", "sapphire_étoilé_taillé", 0,true,0),
        AIGUE_MARINE("Luxe", "💎", "aigue_marine", 0,false,0),
        OPALE("Luxe", "💎", "opale", 0,false,0),
        PERLE_NOIRE("Luxe", "💎", "perle_noire", 0,false,0),
        TOPAZE("Luxe", "💎", "topaze", 0,false,0),
        AIGUE_MARINE_TAILLÉ("Luxe Taillé", "💎", "aigue_marine_taillé", 0,true,0),
        OPALE_TAILLÉ("Luxe Taillé", "💎", "opale_taillé", 0,true,0),
        PERLE_NOIRE_TAILLÉ("Luxe Taillé", "💎", "perle_noire_taillé", 0,true,0),
        TOPAZE_TAILLÉ("Luxe Taillé", "💎", "topaze_taillé", 0,true,0),

        //LINGOTS

        ACIER_INGOT("Lingot", "🔧", "Lingot - acier", 0,true,56),
        ADAMANTIUM_INGOT("Lingot", "🔧", "Lingot - adamantium", 0,true,5006),
        ARGENT_INGOT("Lingot", "🔧", "Lingot - argent", 0,true,146),
        CUIVRE_INGOT("Lingot", "🔧", "Lingot - cuivre", 0,true,66)   ,
        FER_FROID_INGOT("Lingot", "🔧", "Lingot - fer_froid", 0,true,146),
        FER_INGOT("Lingot", "🔧", "Lingot - fer", 0,true,56),
        MITHRAL_INGOT("Lingot", "🔧", "Lingot - mithral", 0,true,5006),
        OR_INGOT("Lingot", "🔧", "Lingot - or", 0,true,106),
        ORICHALQUE_INGOT("Lingot", "🔧", "Lingot - orichalque", 0,true,20006),
        PLATINE_INGOT("Lingot", "🔧", "Lingot - platine", 0,true,1006),

        //RESSOURCE DE LUXES/LUXUEUSES

        BAMBOO("Luxueuse", "🔧", "bamboo", 0,false,0),
        CAFÉ("Luxueuse", "🔧", "café", 0,false,0),
        EBENITE("Luxueuse","🔧", "Ebénite",2501, false, 0),
        EBENITEPLANK("Luxueuse", "🔧", "Planche d'ébénite", 0, true,5006 ),
        ENCENS("Luxueuse", "🔧", "encens", 0,false,0),
        EPICES("Luxueuse", "🔧", "epices", 0,false,0),
        VIN("Luxueuse", "🔧", "vin", 0,true,0),
        SEL("Luxueuse", "🔧", "sel", 0,false,0),


        //MINERAIS

        ADAMANTIUM("Minerais", "🔧", "adamantium", 2501,false,0),
        ARGENT("Minerais", "🔧", "argent", 101,false,0),
        CHARBON("Minerais", "🔧", "charbon", 25,false,0),
        CUIVRE("Minerais", "🔧", "cuivre", 30,false,0),
        FER_FROID("Minerais", "🔧", "fer_froid", 0,false,0),
        FER("Minerais", "🔧", "fer", 26,false,0),
        MITHRAL("Minerais", "🔧", "mithral", 2501,false,0),
        OR("Minerais", "🔧", "or", 81,false,0),
        ORICHALQUE("Minerais", "🔧", "orichalque", 10001,false,0),
        PLATINE("Minerais", "🔧", "platine", 501,false,0),

        //NOURRITURE

        NOURRITURE("Nourriture", "🌾", "nourriture", 5.1,false,0),
        AIL("Nourriture", "🌾", "ail", 0,false,0),
        ARTICHAUT("Nourriture", "🌾", "artichaut", 0,false,0),
        AUBERGINE("Nourriture", "🌾", "aubergine", 0,false,0),
        AVOINE("Nourriture", "🌾", "avoine", 0,false,0),
        BETTERAVE_SUCRIÈRE("Nourriture", "🌾", "betterave_sucrière", 0,false,0),
        BETTERAVE("Nourriture", "🌾", "betterave", 0,false,0),
        BLÉ("Nourriture", "🌾", "blé", 0,false,0),
        BLETTE("Nourriture", "🌾", "blette", 0,false,0),
        CAROTTE("Nourriture", "🌾", "carotte", 0,false,0),
        CÉLERI("Nourriture", "🌾", "céleri", 0,false,0),
        CHAMPIGNONS("Nourriture", "🌾", "champignons", 0,false,0),
        CHOUX("Nourriture", "🌾", "choux", 0,false,0),
        CONCOMBRE("Nourriture", "🌾", "concombre", 0,false,0),
        COURGE("Nourriture", "🌾", "courge", 0,false,0),
        COURGETTE("Nourriture", "🌾", "courgette", 0,false,0),
        CRESSON("Nourriture", "🌾", "cresson", 0,false,0),
        ECHALOTE("Nourriture", "🌾", "echalote", 0,false,0),
        ENDIVE("Nourriture", "🌾", "endive", 0,false,0),
        EPINARD("Nourriture", "🌾", "epinard", 0,false,0),
        FENOUIL("Nourriture", "🌾", "fenouil", 0,false,0),
        FÈVES("Nourriture", "🌾", "fèves", 0,false,0),
        FOURRAGE("Nourriture", "🌾", "fourrage", 0,false,0),
        FRUITS("Nourriture", "🌾", "fruits", 0,false,0),
        GRAINES_DE_CANARI("Nourriture", "🌾", "graines_de_canari", 0,false,0),
        HARICOT("Nourriture", "🌾", "haricot", 0,false,0),
        LAITUE("Nourriture", "🌾", "laitue", 0,false,0),
        LARME_DE_JOB("Nourriture", "🌾", "larme_de_job", 0,false,0),
        LENTILLES("Nourriture", "🌾", "lentilles", 0,false,0),
        MELON("Nourriture", "🌾", "melon", 0,false,0),
        MILLET("Nourriture", "🌾", "millet", 0,false,0),
        NAVETS("Nourriture", "🌾", "navets", 0,false,0),
        OEUFS_DE_CANARD("Nourriture", "🌾", "oeufs_de_canard", 0,false,0),
        OEUFS_DE_POULE("Nourriture", "🌾", "oeufs_de_poule", 0,false,0),
        OIGNONS("Nourriture", "🌾", "oignons", 0,false,0),
        ORGE("Nourriture", "🌾", "orge", 0,false,0),
        PANAIS("Nourriture", "🌾", "panais", 0,false,0),
        PERSIL("Nourriture", "🌾", "persil", 0,false,0),
        POIREAUX("Nourriture", "🌾", "poireaux", 0,false,0),
        POIS("Nourriture", "🌾", "pois", 0,false,0),
        POISSON("Nourriture", "🌾", "poisson", 0,false,0),
        POIVRON("Nourriture", "🌾", "poivron", 0,false,0),
        POMME_DE_TERRE("Nourriture", "🌾", "pomme_de_terre", 0,false,0),
        POTIRON("Nourriture", "🌾", "potiron", 0,false,0),
        RADIS("Nourriture", "🌾", "radis", 0,false,0),
        RAISIN("Nourriture", "🌾", "raisin", 0,false,0),
        RIZ("Nourriture", "🌾", "riz", 0,false,0),
        SEIGLE("Nourriture", "🌾", "seigle", 0,false,0),
        TEFF_ABYSSINIE("Nourriture", "🌾", "teff_d'abyssinie", 0,false,0),
        TOMATE("Nourriture", "🌾", "tomate", 0,false,0),
        TRITICALE("Nourriture", "🌾", "triticale", 0,false,0),
        VIANDE_DE_CANARD("Nourriture", "🦆", "viande_de_canard", 0,false,0),
        VIANDE_DE_CHÈVRES("Nourriture", "🐏", "viande_de_chèvres", 0,false,0),
        VIANDE_DE_MOUTONS("Nourriture", "🔧", "viande_de_moutons", 0,false,0),
        VIANDE_DE_PORC("Nourriture", "🐖", "viande_de_porc", 0,false,0),
        VIANDE_DE_POULE("Nourriture", "🐓", "viande_de_poule", 0,false,0),
        VIANDE_DE_VACHE("Nourriture", "🐄", "viande_de_vache", 0,false,0),
        VIANDE_DE_POULE_TRAVAILLÉ("Nourriture Transformé", "🍽️", "viande_de_poule_travaillé", 0,true,0),
        VIANDE_DE_CANARD_TRAVAILLÉ("Nourriture Transformé", "🍽️", "viande_de_canard_travaillé", 0,true,0),
        VIANDE_DE_CHÈVRES_TRAVAILLÉ("Nourriture Transformé", "🍽️", "viande_de_chèvres_travaillé", 0,true,0),
        VIANDE_DE_MOUTONS_TRAVAILLÉ("Nourriture Transformé", "🍽️", "viande_de_moutons_travaillé", 0,true,0),
        VIANDE_DE_PORC_TRAVAILLÉ("Nourriture Transformé", "🍽️", "viande_de_porc_travaillé", 0,true,0),
        VIANDE_DE_VACHE_TRAVAILLÉ("Nourriture Transformé", "🍽️", "viande_de_vache_travaillé", 0,true,0),
        FARINE_DAVOINE("Nourriture Transformé", "🔧", "farine_d'avoine", 0,true,0),
        FARINE_DE_BLÉ("Nourriture Transformé", "🔧", "farine_de_blé", 0,true,0),
        FARINE_DE_GRAINES_DE_CANARI("Nourriture Transformé", "🔧", "farine_de_graines_de_canari", 0,true,0),
        FARINE_DE_LARME_DE_JOB("Nourriture Transformé", "🔧", "farine_de_larme_de_job", 0,true,0),
        FARINE_DE_MILLETS("Nourriture Transformé", "🔧", "farine_de_millets", 0,true,0),
        FARINE_DORGE("Nourriture Transformé", "🔧", "farine_d'orge", 0,true,0),
        FARINE_DE_RIZ("Nourriture Transformé", "🔧", "farine_de_riz", 0,true,0),
        FARINE_DE_SEIGLE("Nourriture Transformé", "🔧", "farine_de_seigle", 0,true,0),
        FARINE_DE_TEFF_DABYSSINIE("Nourriture Transformé", "🔧", "farine_de_teff_d'abyssinie", 0,true,0),
        FARINE_DE_TRITICALE("Nourriture Transformé", "🔧", "farine_de_triticale", 0,true,0),
        BIÈRE("Nourriture Transformé", "🍺", "bière", 0,true,0),
        BEURRE("Nourriture Transformé", "🧈", "beurre", 0,true,0),
        FROMAGE_À_PÂTE_FRAÎCHE("Nourriture Transformé", "🧀", "les_fromage_à_pâte_fraîche", 0,true,0),
        FROMAGES_À_PÂTE_MOLLE_ET_CROÛTE_FLEURIE("Nourriture Transformé", "🧀", "les_fromages_à_pâte_molle_et_croûte_fleurie", 0,true,0),
        FROMAGES_À_PÂTE_MOLLE_ET_CROÛTE_LAVÉE("Nourriture Transformé", "🧀", "les_fromages_à_pâte_molle_et_croûte_lavée", 0,true,0),
        FROMAGES_À_PÂTE_PERSILLÉE("Nourriture Transformé", "🧀", "les_fromages_à_pâte_persillée", 0,true,0),
        FROMAGES_À_PÂTE_PRESSÉE_CUITE("Nourriture Transformé", "🧀", "les_fromages_à_pâte_pressée_cuite", 0,true,0),
        FROMAGES_AU_LAIT_DE_CHÈVRE("Nourriture Transformé", "🧀", "les_fromages_au_lait_de_chèvre", 0,true,0),
        FROMAGES_FONDUS("Nourriture Transformé", "🧀", "les_fromages_fondus", 0,true,0),
        PAIN("Nourriture Transformé", "🔧", "pain", 0,true,0),
        FROMAGES_À_PÂTE_PRESSÉE_NON_CUITE("Nourriture Transformé", "🔧", "les_fromages_à_pâte_pressée_non-cuite", 0,true,0),
        PLAT_CUISINÉ_NOURRITURE("Nourriture Transformé"," 🍲", "plat_cuisiné_(nourriture)", 0,true,0),

        //SOUS-PRODUITS NOURRITURE

        LAIT("Sous-Produits", "🔧", "lait", 0,false,0),
        LAINE_BRUTE("Sous-Produits", "🔧", "laine_brute", 0,false,0),
        PAILLE("Sous-Produits", "🔧", "paille", 0,false,0),

        //PRODUITS TRANSFORMES

        PIERRE_TAILLÉE("Transformé", "🔧", "pierre_taillée", 0,true,0),
        CORDAGE("Transformé", "🔧", "cordage", 0,true,0),
        CUIR("Transformé", "🔧", "cuir", 0,true,0),
        LAINE_RAFFINÉE("Transformé", "🔧", "laine_raffinée", 0,true,0),
        PAPIER("Transformé", "🔧", "papier", 0,true,0),
        PLANCHE_DEBÉNITE("Transformé", "🔧", "planche_d'ebénite", 0,true,0),
        PLANCHES_DE_BOIS("Transformé", "🔧", "planches_de_bois", 0,true,0),
        PLAT_CUISINÉ_OR("Transformé", "🔧", "plat_cuisiné_(or)", 0,true,0),
        TISSUS("Transformé", "🔧", "tissus", 0,true,0),
        VERRE("Transformé", "🔧", "verre", 0,true,0),
        FLÈCHES("Transformé", "🔧", "flèches", 0,true,0),
        VÊTEMENT_DE_BASE("Transformé", "🔧", "vêtement_de_base", 0,true,0),
        VÊTEMENT_CHAUDS("Transformé", "🔧", "vêtement_chauds", 0,true,0),
        VÊTEMENT_ORNEMENTÉ("Transformé", "🔧", "vêtement_ornementé", 0,true,0),
        VÊTEMENT_OSTENTATOIRE("Transformé", "🔧", "vêtement_ostentatoire", 0,true,0),
        VÊTEMENT_LUXUEUX("Transformé", "🔧", "vêtement_luxueux", 0,true,0),
        VÊTEMENT_ROYAUX("Transformé", "🔧", "vêtement_royaux", 0,true,0);

        private final String category;
        private final String name;
        private final String catIcon;
        private final double value;
        private final boolean transformed;
        private final double valueIfTransformed;
        private double foodValue;

        ResourceType(String category, String catIcon, String name,  double value, boolean transformed, double valueIfTransformed){
            this.category = category;
            this.name = name;
            this.catIcon = catIcon;
            this.value = value;
            this.transformed = transformed;
            this.valueIfTransformed = valueIfTransformed;
        }
        ResourceType(String category, String catIcon, String name,  double value, boolean transformed, double valueIfTransformed, double foodValue){
            this.category = category;
            this.name = name;
            this.catIcon = catIcon;
            this.value = value;
            this.transformed = transformed;
            this.valueIfTransformed = valueIfTransformed;
            this.foodValue = foodValue;
        }


        /// NEED TO REFLECT FOR THE SDN
        public String getCategory() { return category; }
        public String getName() { return name; }
        public String getIcon() { return catIcon; }
        public double getBaseValue() { return value; }
        public boolean getTransformed() {return transformed; }
        public double getvalueIfTransformed() { return valueIfTransformed; }
        public static String getIconForResource(String resourceName) {
            for (ResourceType rt : ResourceType.values()) {
                if (rt.getName().equalsIgnoreCase(resourceName)) {
                    return rt.getIcon();
                }
            }
            return ""; // fallback if not found
        }
        private double getFoodValue() { return foodValue; }
        private static final Map<String,ResourceType> BY_NAME =
                Arrays.stream(values())
                        .collect(Collectors.toMap(
                                rt->rt.name,
                                rt->rt));

        public static ResourceType lookupByName(String name) {
            return BY_NAME.getOrDefault(name, null);}
        @Override
        public String toString() { return name; }
    }

    public enum Workers {
        ALCHIMISTE("Artisan","Alchimiste",  8.5, 1.0, Set.of(AuxBuilding.AUX_6_0)),
        ARMURIER("Artisan","Armurier", 5, 1.0, Set.of(AuxBuilding.AUX_6_0)),
        CHARPENTIER("Artisan","Charpentier",0.1,1, Set.of(AuxBuilding.AUX_6_0)),
        COUTELIER("Artisan","Coutelier",1.5,1,Set.of(AuxBuilding.AUX_6_0)),
        FACTEURARC("Artisan","Facteur d'arc",5,1,Set.of(AuxBuilding.AUX_6_0)),
        MFERRAND("Artisan","Maréchal-férrand",1.5,1,Set.of(AuxBuilding.AUX_6_0)),
        FONDEUR("Artisan","Fondeur",5,1,Set.of(AuxBuilding.AUX_6_0)),
        BELLFONDEUR("Artisan","Fondeur de Cloche",5,1,Set.of(AuxBuilding.AUX_6_0)),
        FORGERON("Artisan","Forgeron",2.5,1,Set.of(AuxBuilding.AUX_6_0)),
        WEAPSMITH("Artisan","Forgeur d'Arme",2.5,1,Set.of(AuxBuilding.AUX_6_0)),
        GRAVEUR("Artisan","Graveur",5,1,Set.of(AuxBuilding.AUX_6_0)),
        JOAILLER("Artisan","Joailler",5,1,Set.of(AuxBuilding.AUX_6_0)),
        TAILLEUR("Artisan","Tailleur",1,1,Set.of(AuxBuilding.AUX_6_0)),
        TANNEUR("Artisan","Tanneur",5,1,Set.of(AuxBuilding.AUX_6_0)),
        VIGNERON("Artisan","Vigneron",2.5,1,Set.of(AuxBuilding.AUX_6_0)),

        ARCHEOLOGUE("Autre", "Archéologue", 10,1,Set.of(MainBuilding.MAIN_00)),
        ASTROLOGISTE("Autre", "Astrologiste",3,1,Set.of(MainBuilding. MAIN_16_0)),
        AVOCAT("Autre", "Avocat",2,1,Set.of(MainBuilding.MAIN_1_0)),
        BARBIER("Autre", "Barbier",1 ,1,Set.of(MainBuilding.MAIN_1_0)),
        CAPITAINE("Autre", "Capitaine de Navire", 0,1,Set.of(MainBuilding.MAIN_1_0)),
        DOCTEUR("Autre", "Docteur",3,1,Set.of(MainBuilding.MAIN_1_0)),
        MARIN("Autre","Marin", 0.05,0.1, Set.of(MainBuilding.MAIN_1_0)),
        NAVIGATEUR("Autre", "Navigateur",0.05,1,Set.of(MainBuilding.MAIN_1_0)),
        PROSPECTUER("Autre", "Prospecteur", 5,1, Set.of(MainBuilding.MAIN_1_0)),
        RELIEUR("Autre", "Relieur",1,1, Set.of(AuxBuilding.AUX_17_0)),
        VETERINAIRE("Autre", "Vétérinaire",5,1,Set.of(MainBuilding.MAIN_1_0)),

        ARCHITECTE("Bureau d'Intendance","Architecte",5,1,Set.of(MainBuilding.MAIN_1_0)),
        CLERC("Bureau d'Intendance",   "Clerc",0.2,0.5,Set.of(MainBuilding.MAIN_1_0)),
        INTENDANT("Bureau d'Intendance",  "Intendant",0.7,1,Set.of(MainBuilding.MAIN_1_0)),
        QUARTERMASTER("Bureau d'Intendance",   "Contre-maître",0.7,1,Set.of(MainBuilding.MAIN_1_0)),
        MERCHANTDDL("Bureau d'Intendance",   "Marchand des Cours du Donjon",5,1,Set.of(MainBuilding.MAIN_1_0)),

        AMBASSADOR("Diplomatie","Ambassadeur",6,1,Set.of(MainBuilding.MAIN_1111)),
        HERALD("Diplomatie","Herault",8.8,1,Set.of(MainBuilding.MAIN_1111)),
        INTERPRET("Diplomatie","Interprête",2,1,Set.of(MainBuilding.MAIN_1111)),
        MESSAGER("Diplomatie","Messager",3,1,Set.of(MainBuilding.MAIN_1111)),


        APPRENTIS("Etude",  "Apprentis",1.5,1,Set.of(MainBuilding.MAIN_1_0, MainBuilding.MAIN_1111)),
        COPISTES("Etude",   "Professeur",5,1,Set.of(MainBuilding.MAIN_1_0, MainBuilding.MAIN_1111)),
        MAGE("Etude", "Mage", 0,2,Set.of(MainBuilding.MAIN_1_0, MainBuilding.MAIN_1111)),
        PRIEST("Etude", "Prêtre", 0,2,Set.of(MainBuilding.MAIN_1_0, MainBuilding.MAIN_1111)),
        SCRIBE("Etude",   "Scribe",2,1,Set.of(MainBuilding.MAIN_1_0, MainBuilding.MAIN_1111)),
        // WTF IDK //SCULPTEUR("Etude",   "Sculpteur",4,1,Set.of(MainBuilding.MAIN_1_0, MainBuilding.MAIN_1111)),

        ASSASSIN    ("Infomation", "Assassin",0,2,Set.of(MainBuilding.MAIN_1_0, MainBuilding.MAIN_1111)),
        ESPION("Infomation", "Espion",	0,2,Set.of(MainBuilding.MAIN_1_0, MainBuilding.MAIN_1111)),
        MENDIANT("Infomation", "Mendiant",0.01,0.01,Set.of(MainBuilding.MAIN_1_0)),
        MENESTREL("Infomation", "Ménestrel",	1,1,Set.of(MainBuilding.MAIN_1_0)),

        SCAV("Special", "Récupérateur",12,1,Set.of(), true, "hci"),
        STRATEGE("Special", "Stratège",5,1,Set.of(), true, "decimus"),

        HOTELIER("Place-Forte","Hotelier",1,1,Set.of(MainBuilding.MAIN_1111)),
        SQUIRE("Place-Forte" ,"Palefrenier",0.02,0.1,Set.of(MainBuilding.MAIN_1111)),
        PORTIER("Place-Forte","Portier", 0.5,0.5,Set.of(MainBuilding.MAIN_1111)),
        TEACHER("Place-Forte", "Professeur", 5,1,Set.of(MainBuilding.MAIN_1_0, MainBuilding.MAIN_1111)),

        ARROWMAKERS("Production", "Flêcheur", 0.5,0.5,Set.of(AuxBuilding.AUX_17_0)),
        BOATMAKER("Production", "Charpentier (bateaux)", 0.4, 0.5,Set.of(AuxBuilding.AUX_17_0)),
        PAINTER("Production","Peintre", 2,1,Set.of(AuxBuilding.AUX_17_0)),
        SAGE("Production", "Sage",2.0,1,Set.of(AuxBuilding.AUX_17_0)),
        LOCKSMITH("Production", "Serrurier", 1,1,Set.of(AuxBuilding.AUX_17_0)),

        STONECUTTER("Récolte","Tailleur de Pierre", 0.5,1,Set.of(MainBuilding.MAIN_4_0, MainBuilding.MAIN_4_1, MainBuilding.MAIN_4_2 )),
        BERGER("Récolte","Berger",0.05,1,Set.of(MainBuilding.MAIN_29_0)),
        BUCHERON("Récolte","Bûcheron",0.4,1,Set.of(MainBuilding.MAIN_26_0)),
        CHASSEUR("Récolte", "Chasseur",2.5,1,Set.of(MainBuilding.MAIN_30_0)),
        MINERSCOMP("Récolte", "Compagnie de Mineur",1,1,Set.of(MAIN_12_0, MAIN_12_1, MAIN_12_2)),
        FREEFARMER("Récolte", "Fermier (libre)",0.02,0,Set.of(MainBuilding.MAIN_28_0, MainBuilding.MAIN_28_1, MainBuilding.MAIN_28_2, MainBuilding.MAIN_11_0,MainBuilding.MAIN_11_1,MainBuilding.MAIN_11_2)),
        SERFFARMER("Récolte", "Fermier (serf)",0.01,0,Set.of(MainBuilding.MAIN_28_0, MainBuilding.MAIN_28_1, MainBuilding.MAIN_28_2, MainBuilding.MAIN_11_0,MainBuilding.MAIN_11_1,MainBuilding.MAIN_11_2)),
        FORESTER("Récolte", "Forestier",0.3,1,Set.of(MainBuilding.MAIN_26_0)),
        GARDENER("Récolte",    "Jardinier",0.05,1,Set.of(MainBuilding.MAIN_23_0, MainBuilding.MAIN_24_0)),
        FISHERMAN("Récolte",   "Pécheur",0.5,0,Set.of(MainBuilding.MAIN_27_0)),

        MISSIONARY("Religieux","Missionnaire",1,1,Set.of(AuxBuilding.AUX_4_0, AuxBuilding.AUX_3_0, AuxBuilding.AUX_9_0, AuxBuilding.AUX_9_1, AuxBuilding.AUX_9_2)),
        PRIEST2("Religieux","Prêtre", 0, 2, Set.of(AuxBuilding.AUX_4_0, AuxBuilding.AUX_3_0, AuxBuilding.AUX_9_0, AuxBuilding.AUX_9_1, AuxBuilding.AUX_9_2)),

        APOTHICAIRE("Transformation", "Apothicaire", 2.5,1,Set.of(AuxBuilding.AUX_6_0)),
        BOUCHER("Transformation", "Boucher", 0.5,1,Set.of(AuxBuilding.AUX_6_0)),
        BOULANGER("Transformation", "Boulanger", 0.5,1,Set.of(AUX_1_0)),
        MEUNIER("Transformation", "Meunier",0.5,1,Set.of(AUX_1_0)),
        CARTOGRAPHE("Transformation", "Cartographe",5,1,Set.of(AuxBuilding.AUX_17_0)),
        COUTURIERE("Transformation", "Coûturière",1,1,Set.of(AuxBuilding.AUX_17_0)),
        CUISINIER("Transformation", "Cuisinier",2,1,Set.of(AuxBuilding.AUX_7_0)),
        FROMAGER("Transformation", "Fromager",3,1,Set.of(AuxBuilding.AUX_14_0)),
        MACON("Transformation", "Maçon",0.5,1,Set.of(AuxBuilding.AUX_8_0)),
        PAPETIER("Transformation", "Papetier", 5,1,Set.of(AuxBuilding.AUX_17_0)),
        POTIER("Transformation", "Potier",0.25,1,Set.of(AuxBuilding.AUX_17_0)),
        TRAPPEUR("Transformation", "Trappeur",0.5,1,Set.of(MainBuilding.MAIN_30_0)),
        VERRIER("Transformation", "Verrier",2.5,1, Set.of(AuxBuilding.AUX_17_0)),
        BRASSEUR("Transformation", "Brasseur",1,1,Set.of(AuxBuilding.AUX_17_0)),
        PLANCHEUR("Transformation", "Plancheur",2,1,Set.of(AuxBuilding.AUX_17_0)),

        OUVRIER("Utilitaire", "Ouvrier", 0.01,0.01,Set.of(MainBuilding.MAIN_1_0)),
        MAGE2("Magie", "Mages",0,2,Set.of(MainBuilding.MAIN_1_0)),


        ;
        private final String category;
        private final String jobName;
        private final double baseSalary;
        private final double bFC ; //bFC == baseFoodConsumption
        private final Set<JobBuilding> workBuildings;
        private String factionId;
        private boolean isFactionSpecific;

        private static int currentTurn = 0;
        private static final Map<Workers, Double> turnSalaries = new HashMap<>();
        private static final Map<Workers, Double> turnFoodConsumption = new HashMap<>();
        private static final Random randomSetSalary = new Random();
        private double randomSalary;

        Workers(String category, String jobName, double baseSalary, double bFC, Set<JobBuilding> workBuildings) {
            this.category = category;
            this.jobName = jobName;
            this.baseSalary = baseSalary;
            this.bFC = bFC;
            this.workBuildings = workBuildings;
        }
        Workers(String category, String jobName, double baseSalary, double bFC, Set<JobBuilding> workBuildings, boolean isFactionSpecific, String factionId) {
            this.category = category;
            this.jobName = jobName;
            this.baseSalary = baseSalary;
            this.bFC = bFC;
            this.workBuildings = workBuildings;
            this.isFactionSpecific = isFactionSpecific;
            this.factionId = factionId;
        }
        public double getCurrentSalary() {
            return turnSalaries.getOrDefault(this, baseSalary);
        }
        public double getCurrentFoodConsumption() { return turnFoodConsumption.getOrDefault(this, bFC); }
        // Getters pour les valeurs de base (constantes)
        public double getBaseSalary() { return baseSalary; }
        public double getBaseFoodConsumption() { return bFC; }
        public String getCategory() { return category; }
        public String getJobName() { return jobName; }
        public Set<JobBuilding> getWorkBuildings() { return workBuildings; }
        public boolean isFactionSpecific() { return isFactionSpecific; }
        public String getWorkerFactionId() { return factionId; }
    }

    public enum LivestockData {
        CANARD("Canard", "Accès eau", 0.5, 0.8, 1.5, 18,0, 6, 1, 50, true),
        CHEVRE("Chèvre", "Ferme Blé / Biome montagne", 3.0, 1.0, 2.0, 10,0, 4, 1, 25, false),
        MOUTON("Mouton", "Ferme Blé / Biome plaine", 4.0, 0.9, 1.8, 9,0, 4, 1, 50, false),
        PORC("Porc", "Ferme légume", 5.0, 1.0, 2.0, 6.5, 0,3, 1, 25, false),
        POULE("Poule", "Ferme Blé / Attire prédateurs", 0.25, 0.1, 0.15, 45,0, 1, 1, 30, true),
        VACHE("Vache", "Ferme Blé", 8.0, 18.0, 24.0, 27, 0,12, 1, 50, false);

        private final String name;
        public final String condition;
        private final double prixUnite;
        private final double besoinMin;
        private final double besoinMax;
        private final double semainesGrossesse;
        private final double sdnSemaine;
        private final int toursPourMaturite;
        private final int bergersParGroupe;
        private final int animauxParBerger;
        private final boolean attirePredateurs;

        // Constructeur et getters...
        LivestockData(String name, String condition, double prixUnite, double besoinMin,
                      double besoinMax, double sdnSemaine, double semainesGrossesse, int toursPourMaturite,
                      int bergersParGroupe, int animauxParBerger, boolean attirePredateurs) {
            this.name = name;
            this.condition = condition;
            this.prixUnite = prixUnite;
            this.besoinMin = besoinMin;
            this.besoinMax = besoinMax;
            this.sdnSemaine = sdnSemaine;
            this.semainesGrossesse = semainesGrossesse;
            this.toursPourMaturite = toursPourMaturite;
            this.bergersParGroupe = bergersParGroupe;
            this.animauxParBerger = animauxParBerger;
            this.attirePredateurs = attirePredateurs;
        }

        // Getters
        public String getName() {
            return name;
        }
        public String getCondition() {
            return condition;
        }
        public double getPrixUnite() {
            return prixUnite;
        }
        public double getBesoinMin() {
            return besoinMin;
        }
        public double getBesoinMax() {
            return besoinMax;
        }
        public double  getSdnSemaine() { return sdnSemaine; }
        public double getSemainesGrossesse() {
            return semainesGrossesse;
        }
        public int getToursPourMaturite() {
            return toursPourMaturite;
        }
        public int getBergersParGroupe() {
            return bergersParGroupe;
        }
        public int getAnimauxParBerger() {
            return animauxParBerger;
        }
        public boolean isAttirePredateurs() {
            return attirePredateurs;
        }
        public double getCoutReproducteur() {
            return prixUnite * 10.0; // Le reproducteur coûte 10x le prix d'une bête
        }

        public boolean canEstablishIn(SafeHexDetails hex, IHexRepository repo) {
            // Parse les conditions multiples (ex: "FB/Biome montagne")
            String[] conditions = condition.split("/");

            for (String singleCondition : conditions) {
                if (checkSingleCondition(singleCondition.trim(), hex, repo)) {
                    return true; // Une seule condition suffit (OR logic)
                }
            }
            return false;
        }
        private boolean checkSingleCondition(String condition, SafeHexDetails hex, IHexRepository repo) {
            switch (condition.toLowerCase()) {
                case "ferme blé":
                case "fb":
                    return hasWheatFarm(hex);

                case "ferme légume":
                case "ferme legume":
                    return hasVegetableFarm(hex);

                case "accès eau":
                case "acces eau":
                    return hasWaterAccess(hex, repo);

                case "biome montagne":
                    return isMountainBiome(hex);

                case "biome plaine":
                    return isPlainBiome(hex);

                case "attire prédateurs":
                case "attire predateurs":
                    return true; // C'est un avertissement, pas une restriction

                default:
                    return true; // Condition non reconnue = pas de restriction
            }
        }

        // Méthodes de vérification spécifiques
        private boolean hasWheatFarm(SafeHexDetails hex) {
            // Vérifier si l'hex a une ferme de céréales (blé/riz)
            DATABASE.JobBuilding mainBuilding = UIHelpers.getBuildingFromHex(hex, "main");

            if (mainBuilding instanceof DATABASE.MainBuilding) {
                String buildingName = mainBuilding.getBuildName().toLowerCase();
                return buildingName.contains("ferme de céréale") ||
                        buildingName.contains("ferme de blé") ||
                        buildingName.contains("riz");
            }
            return false;
        }
        private boolean hasVegetableFarm(SafeHexDetails hex) {
            // Vérifier si l'hex a une ferme de légumes
            DATABASE.JobBuilding mainBuilding = UIHelpers.getBuildingFromHex(hex, "main");

            if (mainBuilding instanceof DATABASE.MainBuilding) {
                String buildingName = mainBuilding.getBuildName().toLowerCase();
                return buildingName.contains("ferme de légume") ||
                        buildingName.contains("légume");
            }
            return false;
        }
        private boolean hasWaterAccess(SafeHexDetails hex, IHexRepository repo) {
            // Option 1: Vérifier par la clé de l'hex si c'est près d'eau
            String hexKey = hex.getHexKey();

            // Option 2: Vérifier s'il y a un camp de pêcheurs ou accès eau
            DATABASE.JobBuilding mainBuilding = UIHelpers.getBuildingFromHex(hex, "main");
            if (mainBuilding instanceof DATABASE.MainBuilding) {
                String buildingName = mainBuilding.getBuildName().toLowerCase();
                if (buildingName.contains("camp de pêcheurs") ||
                        buildingName.contains("pêche")) {
                    return true;
                }
            }

            // Option 3: Logique basée sur les coordonnées de l'hex
            return isNearWater(hexKey, repo);
        }
        private boolean isMountainBiome(SafeHexDetails hex) {
            // Logique pour détecter si l'hex est en montagne
            // Vous pourriez utiliser une Map de biomes ou analyser la clé de l'hex
            String hexKey = hex.getHexKey();

            // Exemple basé sur les coordonnées (à adapter selon votre système)
            if (hexKey.matches(".*[Mm]ont.*") || hexKey.matches(".*[Hh]ill.*")) {
                return true;
            }

            // Ou utiliser une méthode dans votre système de monde
            return checkBiomeType(hexKey, "mountain");
        }
        private boolean isPlainBiome(SafeHexDetails hex) {
            // Logique similaire pour les plaines
            String hexKey = hex.getHexKey();

            if (hexKey.matches(".*[Pp]lain.*") || hexKey.matches(".*[Pp]rairie.*")) {
                return true;
            }

            return checkBiomeType(hexKey, "plain");
        }
        private boolean isNearWater(String hexKey, IHexRepository repo) {

            return UIHelpers.checkWaterProximity(hexKey, repo);
        }
        private boolean checkBiomeType(String hexKey, String biomeType) {
            // Interface avec votre système de monde/biomes
            // return YourWorldSystem.getBiomeType(hexKey).equals(biomeType);
            return false; // Placeholder
        }

    }

    public static final Map<String, Set<JobBuilding>> JOB_TO_BUILDINGS = new HashMap<>();
    public static final Map<JobBuilding, BuildingProduction> BUILDING_PRODUCTION_DATA = DATABASE.buildProductionData();

    public static Map<JobBuilding, BuildingProduction> buildProductionData() {
        return null;
    }

    private static void initializeJobBuildingRelations() {
        addJobToBuildings("Compagnie de Mineur",
                MAIN_12_0, MAIN_12_1, MAIN_12_2,
                MainBuilding.MAIN_3_0, MainBuilding.MAIN_3_1);

        addJobToBuildings("Fermier (serf)",
                MainBuilding.MAIN_11_0, MainBuilding.MAIN_11_1, MainBuilding.MAIN_11_2);

        addJobToBuildings("Fermier (libre)",
                MainBuilding.MAIN_11_0, MainBuilding.MAIN_11_1, MainBuilding.MAIN_11_2);

        addJobToBuildings("Fermier (serf)",
                MainBuilding.MAIN_28_0, MainBuilding.MAIN_28_1, MainBuilding.MAIN_28_2);

        addJobToBuildings("Fermier (libre)",
                MainBuilding.MAIN_28_0, MainBuilding.MAIN_28_1, MainBuilding.MAIN_28_2);

        addJobToBuildings("Astrologiste",
                MainBuilding.MAIN_16_0, MainBuilding.MAIN_16_1);

        addJobToBuildings("Tailleur de Pierre",
                MainBuilding.MAIN_4_0, MainBuilding.MAIN_4_1, MainBuilding.MAIN_4_2);

        addJobToBuildings("Plancheur", MainBuilding.MAIN_8_0, MainBuilding.MAIN_8_1);
        addJobToBuildings("Bûcheron", MainBuilding.MAIN_26_0);
        addJobToBuildings("Maçon", MainBuilding.MAIN_15_0, MainBuilding.MAIN_15_1);
        addJobToBuildings("Verrier", MainBuilding.MAIN_19_0, MainBuilding.MAIN_19_1);
        addJobToBuildings("Récupérateur", MainBuilding.MAIN_1_0);
        addJobToBuildings("Pécheur", MainBuilding.MAIN_27_0);
        addJobToBuildings("Berger",MainBuilding.MAIN_29_0);
        addJobToBuildings("Chasseur", MainBuilding.MAIN_30_0);
        addJobToBuildings("Forestier",MainBuilding.MAIN_26_0);
        addJobToBuildings("Jardinier",
                MainBuilding.MAIN_24_0,MainBuilding.MAIN_24_1, MainBuilding.MAIN_23_0,MainBuilding.MAIN_23_1);

    }

    public static class BuildingProduction {
        public final JobBuilding building;
        public final ResourceType[] possibleResources;
        public final double baseProductionRate;
        public final int maxWorkers;

        public BuildingProduction(JobBuilding building, ResourceType[] resources,
                                  double baseRate, int maxWorkers) {
            this.building = building;
            this.possibleResources = resources;
            this.baseProductionRate = baseRate;
            this.maxWorkers = maxWorkers;
        }
    }

    public static final Map<String, List<String>> RESSOURCES = new TreeMap<>();
    static {
        RESSOURCES.put("Animaux", Arrays.asList(
                "Dromadaires", "Chevaux", "Ours", "Lézards Géants", "Araignées Géantes", "Lions de Némée"));
        RESSOURCES.put("Culture", Arrays.asList(
                "Chanvre", "Coton", "Fleurs", "Herbes médicinales"));
        RESSOURCES.put("Gemmes", Arrays.asList(
                "Emeraude", "Petit Diamant", "Petit Rubis", "Sapphire"));
        RESSOURCES.put("Gemmes Taillé", Arrays.asList(
                "Petit Diamant Taillé", "Petit Rubis Taillé", "Sapphire Taillé", "Emeraude Taillé"));
        RESSOURCES.put("Joyaux", Arrays.asList(
                "Emeraude verte brillante", "Grand Diamant", "Grand Rubis", "Rubis étoilé", "Sapphire étoilé"));
        RESSOURCES.put("Joyaux Taillé", Arrays.asList(
                "Emeraude verte brillante Taillé", "Grand Diamant Taillé", "Grand Rubis Taillé", "Rubis étoilé Taillé", "Sapphire étoilé Taillé"));
        RESSOURCES.put("Lingot", Arrays.asList(
                "Acier", "Adamantium", "Argent", "Cuivre", "Fer Froid", "Fer", "Mithral", "Or", "Orichalque", "Platine"));
        RESSOURCES.put("Luxe", Arrays.asList(
                "Aigue Marine", "Opale", "Perle noire", "Topaze"));
        RESSOURCES.put("Luxe Taillé", Arrays.asList(
                "Aigue Marine Taillé", "Opale Taillé", "Perle noire Taillé", "Topaze Taillé"));
        RESSOURCES.put("Luxueuse", Arrays.asList(
                "Bamboo", "Café", "Encens", "Epices", "Vin", "Sel"));
        RESSOURCES.put("Minerais", Arrays.asList(
                "Adamantium", "Argent", "Charbon", "Cuivre", "Fer Froid", "Fer", "Mithral", "Or", "Orichalque", "Platine"));
        RESSOURCES.put("Nourriture", Arrays.asList(
                "Nourriture", "Ail", "Artichaut", "Aubergine", "Avoine", "Betterave sucrière", "Betterave", "Blé", "Blette", "Carotte", "Céleri", "Champignons", "Choux", "Concombre", "Courge", "Courgette", "Cresson", "Echalote", "Endive", "Epinard", "Fenouil", "Fèves", "Fourrage", "Fruits", "Graines de canari", "Haricot", "Laitue", "Larme de job", "Lentilles", "Melon", "Millet", "Navets", "Oeufs de Canard", "Oeufs de Poule", "Oignons", "Orge", "Panais", "Persil", "Poireaux", "Pois", "Poisson", "Poivron", "Pomme de Terre", "Potiron", "Radis", "Raisin", "Riz", "Seigle", "Teff d'abyssinie", "Tomate", "Triticale", "Viande de Canard", "Viande de Chèvres", "Viande de Moutons", "Viande de Porc", "Viande de Poule", "Viande de Vache"));
        RESSOURCES.put("Nourriture Transformé", Arrays.asList(
                "Viande de Poule Travaillé", "Viande de Canard Travaillé", "Viande de Chèvres Travaillé", "Viande de Moutons Travaillé", "Viande de Porc Travaillé", "Viande de Vache Travaillé", "Farine d'Avoine", "Farine de Blé", "Farine de Graines de canari", "Farine de Larme de job", "Farine de Millets", "Farine d'Orge", "Farine de Riz", "Farine de Seigle", "Farine de Teff d'abyssinie", "Farine de Triticale", "Bière", "Beurre", "Les fromage à pâte fraîche", "Les fromages à pâte molle et croûte fleurie", "Les fromages à pâte molle et croûte lavée", "Les fromages à pâte persillée", "Les fromages à pâte pressée cuite", "Les fromages au lait de chèvre", "Les fromages fondus", "Pain", "Les fromages à pâte pressée non-cuite", "Plat Cuisiné (Nourriture)"));
        RESSOURCES.put("Sous-Produits", Arrays.asList(
                "Lait", "Laine Brute", "Paille"));
        RESSOURCES.put("Bloc Nourritures", Arrays.asList(
                "Sac de Nourriture", "Plat Cuisiné"));
        RESSOURCES.put("Transformé", Arrays.asList(
                "Pierre Taillée", "Cordage", "Cuir", "Laine Raffinée", "Papier", "Planche d'Ebénite", "Planches de Bois", "Plat Cuisiné (Or)", "Tissus", "Verre", "Flèches", "Vêtement De Base", "Vêtement Chauds", "Vêtement Ornementé", "Vêtement Ostentatoire", "Vêtement Luxueux", "Vêtement Royaux"));
        RESSOURCES.put("Artisanat", Arrays.asList(
                "Meubles", "Bateaux","Engins de Siège","Bâtiments","Vêtements","Plats Cuisinés","Livres","Armes et Armures","Bijoux","Sortilèges","Objets Magiques","Outils","Parchemins"));
    }

// ========== MÉTHODES D'INITIALISATION ==========
//=================PRODUCTION DATA==========================\\
    private static void initializeMainBuildingResources() {
        // Mines - MAIN_12_X
        MAIN_BUILDING_RESOURCES.put(MAIN_12_0, Arrays.asList(FER, OR, ARGENT, EMERAUDE, PETIT_DIAMANT, PETIT_RUBIS, SAPPHIRE, EMERAUDE_VERTE_BRILLANTE, GRAND_DIAMANT,  GRAND_RUBIS, RUBIS_ÉTOILÉ, SAPPHIRE_ÉTOILÉ, AIGUE_MARINE, OPALE, PERLE_NOIRE, TOPAZE));
        MAIN_BUILDING_RESOURCES.put(MAIN_12_1, Arrays.asList(FER, OR, ARGENT, CUIVRE,  EMERAUDE, PETIT_DIAMANT, PETIT_RUBIS, SAPPHIRE, EMERAUDE_VERTE_BRILLANTE, GRAND_DIAMANT,  GRAND_RUBIS, RUBIS_ÉTOILÉ, SAPPHIRE_ÉTOILÉ, AIGUE_MARINE, OPALE, PERLE_NOIRE, TOPAZE));
        MAIN_BUILDING_RESOURCES.put(MAIN_12_2, Arrays.asList(FER, OR, ARGENT, CUIVRE, MITHRAL,  EMERAUDE, PETIT_DIAMANT, PETIT_RUBIS, SAPPHIRE, EMERAUDE_VERTE_BRILLANTE, GRAND_DIAMANT,  GRAND_RUBIS, RUBIS_ÉTOILÉ, SAPPHIRE_ÉTOILÉ, AIGUE_MARINE, OPALE, PERLE_NOIRE, TOPAZE));

        // Fermes de légumes - MAIN_11_X
        MAIN_BUILDING_RESOURCES.put(MAIN_11_0, Arrays.asList(CAROTTE, NAVETS, POIREAUX));
        MAIN_BUILDING_RESOURCES.put(MAIN_11_1, Arrays.asList(CAROTTE, NAVETS, POIREAUX, EPINARD));
        MAIN_BUILDING_RESOURCES.put(MAIN_11_2, Arrays.asList(CAROTTE, NAVETS, POIREAUX, EPINARD, ARTICHAUT));

        // Fermes de céréales - MAIN_28_X
        MAIN_BUILDING_RESOURCES.put(MAIN_28_0, Arrays.asList(BLÉ, ORGE, AVOINE));
        MAIN_BUILDING_RESOURCES.put(MAIN_28_1, Arrays.asList(BLÉ, ORGE, AVOINE, SEIGLE));
        MAIN_BUILDING_RESOURCES.put(MAIN_28_2, Arrays.asList(BLÉ, ORGE, AVOINE, SEIGLE, RIZ));

        // Carrières - MAIN_3_X et MAIN_4_X
        MAIN_BUILDING_RESOURCES.put(MAIN_3_0, Arrays.asList()); // Argile - TODO: ajouter ResourceType pour argile
        MAIN_BUILDING_RESOURCES.put(MAIN_3_1, Arrays.asList()); // Argile Tier 1
        MAIN_BUILDING_RESOURCES.put(MAIN_4_0, Arrays.asList()); // Pierre - TODO: ajouter ResourceType pour pierre
        MAIN_BUILDING_RESOURCES.put(MAIN_4_1, Arrays.asList()); // Pierre Tier 1
        MAIN_BUILDING_RESOURCES.put(MAIN_4_2, Arrays.asList()); // Pierre Tier 2

        // Scieries - MAIN_8_X
        MAIN_BUILDING_RESOURCES.put(MAIN_8_0, Arrays.asList(PLANCHES_DE_BOIS));
        MAIN_BUILDING_RESOURCES.put(MAIN_8_1, Arrays.asList(PLANCHES_DE_BOIS));

        // Pâturages - MAIN_29_0
        MAIN_BUILDING_RESOURCES.put(MAIN_29_0, Arrays.asList(LAIT, LAINE_BRUTE, VIANDE_DE_VACHE, VIANDE_DE_MOUTONS));

        // Vergers - MAIN_24_X
        MAIN_BUILDING_RESOURCES.put(MAIN_24_0, Arrays.asList(FRUITS));
        MAIN_BUILDING_RESOURCES.put(MAIN_24_1, Arrays.asList(FRUITS, RAISIN));

        // Camp de bûcheron - MAIN_26_0
        MAIN_BUILDING_RESOURCES.put(MAIN_26_0, Arrays.asList(PLANCHES_DE_BOIS, PAILLE));

        // Camp de pêcheurs - MAIN_27_0
        MAIN_BUILDING_RESOURCES.put(MAIN_27_0, Arrays.asList(POISSON));

        // Camp de chasseurs - MAIN_30_0
        MAIN_BUILDING_RESOURCES.put(MAIN_30_0, Arrays.asList(VIANDE_DE_VACHE, CUIR, CHAMPIGNONS));

        // Tenturerie - MAIN_9_X
        MAIN_BUILDING_RESOURCES.put(MAIN_9_0, Arrays.asList(TISSUS));
        MAIN_BUILDING_RESOURCES.put(MAIN_9_1, Arrays.asList(TISSUS, VÊTEMENTS));

        // Observatoire - MAIN_16_X
        MAIN_BUILDING_RESOURCES.put(MAIN_16_0, Arrays.asList()); // Recherche - pas de ressources physiques
        MAIN_BUILDING_RESOURCES.put(MAIN_16_1, Arrays.asList());

        // Saliculture - MAIN_17_X
        MAIN_BUILDING_RESOURCES.put(MAIN_17_0, Arrays.asList(SEL));
        MAIN_BUILDING_RESOURCES.put(MAIN_17_1, Arrays.asList(SEL));

        // Corderie - MAIN_18_X
        MAIN_BUILDING_RESOURCES.put(MAIN_18_0, Arrays.asList(CORDAGE));
        MAIN_BUILDING_RESOURCES.put(MAIN_18_1, Arrays.asList(CORDAGE, FLÈCHES));

        // Atelier de Soufflage de Verre - MAIN_19_X
        MAIN_BUILDING_RESOURCES.put(MAIN_19_0, Arrays.asList(VERRE));
        MAIN_BUILDING_RESOURCES.put(MAIN_19_1, Arrays.asList(VERRE));

        // Torréfacteur - MAIN_20_X
        MAIN_BUILDING_RESOURCES.put(MAIN_20_0, Arrays.asList(CAFÉ));
        MAIN_BUILDING_RESOURCES.put(MAIN_20_1, Arrays.asList(CAFÉ));

        // Four à Goudron - MAIN_21_X
        MAIN_BUILDING_RESOURCES.put(MAIN_21_0, Arrays.asList()); // Goudron - TODO: ajouter ResourceType
        MAIN_BUILDING_RESOURCES.put(MAIN_21_1, Arrays.asList());

        // Tréfilerie - MAIN_22_X
        MAIN_BUILDING_RESOURCES.put(MAIN_22_0, Arrays.asList(OUTILS));
        MAIN_BUILDING_RESOURCES.put(MAIN_22_1, Arrays.asList(OUTILS));

        // Pépinière - MAIN_23_X
        MAIN_BUILDING_RESOURCES.put(MAIN_23_0, Arrays.asList(FLEURS, HERBES_MÉDICINALES));
        MAIN_BUILDING_RESOURCES.put(MAIN_23_1, Arrays.asList(FLEURS, HERBES_MÉDICINALES));

        // Atelier de Marbrier - MAIN_15_X
        MAIN_BUILDING_RESOURCES.put(MAIN_15_0, Arrays.asList(PIERRE_TAILLÉE));
        MAIN_BUILDING_RESOURCES.put(MAIN_15_1, Arrays.asList(PIERRE_TAILLÉE));

        // Thermes - MAIN_14_X (pas de production matérielle)
        MAIN_BUILDING_RESOURCES.put(Main_14_0, Arrays.asList());
        MAIN_BUILDING_RESOURCES.put(MAIN_14_1, Arrays.asList());

        // Oratoire - MAIN_13_X (production spirituelle)
        MAIN_BUILDING_RESOURCES.put(MAIN_13_0, Arrays.asList());
        MAIN_BUILDING_RESOURCES.put(MAIN_13_1, Arrays.asList()); // Avec REG_ARTWORK

        // Entrepôts - pas de production, juste stockage
        MAIN_BUILDING_RESOURCES.put(MAIN_10_0, Arrays.asList());
        MAIN_BUILDING_RESOURCES.put(MAIN_10_1, Arrays.asList());
        MAIN_BUILDING_RESOURCES.put(MAIN_10_2, Arrays.asList());

        // Abbaye et variantes religieuses
        MAIN_BUILDING_RESOURCES.put(MAIN_2_0, Arrays.asList());
        MAIN_BUILDING_RESOURCES.put(MAIN_2_1, Arrays.asList(LIVRES, PARCHEMINS));

        // Commanderie - MAIN_5_X (militaire)
        MAIN_BUILDING_RESOURCES.put(MAIN_5_0, Arrays.asList());
        MAIN_BUILDING_RESOURCES.put(MAIN_5_1, Arrays.asList());

        // Avant-Poste - MAIN_6_0
        MAIN_BUILDING_RESOURCES.put(MAIN_6_0, Arrays.asList());

        // Couvent - MAIN_7_X
        MAIN_BUILDING_RESOURCES.put(MAIN_7_0, Arrays.asList());
        MAIN_BUILDING_RESOURCES.put(MAIN_7_1, Arrays.asList(LIVRES));
    }
    private static void initializeAuxBuildingResources() {
        // Moulins - AUX_1_X et AUX_2_X
        AUX_BUILDING_RESOURCES.put(AUX_1_0, Arrays.asList(FARINE_DE_BLÉ, FARINE_DORGE));
        AUX_BUILDING_RESOURCES.put(AUX_1_1, Arrays.asList(FARINE_DE_BLÉ, FARINE_DORGE, FARINE_DAVOINE));
        AUX_BUILDING_RESOURCES.put(AUX_1_2, Arrays.asList(FARINE_DE_BLÉ, FARINE_DORGE, FARINE_DAVOINE, FARINE_DE_SEIGLE));

        AUX_BUILDING_RESOURCES.put(AUX_2_0, Arrays.asList(FARINE_DE_BLÉ, FARINE_DORGE));
        AUX_BUILDING_RESOURCES.put(AUX_2_1, Arrays.asList(FARINE_DE_BLÉ, FARINE_DORGE, FARINE_DAVOINE));
        AUX_BUILDING_RESOURCES.put(AUX_2_2, Arrays.asList(FARINE_DE_BLÉ, FARINE_DORGE, FARINE_DAVOINE, FARINE_DE_SEIGLE));

        // Cloître et Prieuré - religieux
        AUX_BUILDING_RESOURCES.put(AUX_3_0, Arrays.asList());
        AUX_BUILDING_RESOURCES.put(AUX_4_0, Arrays.asList());

        // Fonderie - AUX_5_X
        AUX_BUILDING_RESOURCES.put(AUX_5_0, Arrays.asList(FER_INGOT, ACIER_INGOT));
        AUX_BUILDING_RESOURCES.put(AUX_5_1, Arrays.asList(FER_INGOT, ACIER_INGOT, OR_INGOT, ARGENT_INGOT));

        // Atelier d'Artisan - AUX_6_0
        AUX_BUILDING_RESOURCES.put(AUX_6_0, Arrays.asList(OUTILS, BIJOUX, VÊTEMENTS));

        // Cuisines - AUX_7_0
        AUX_BUILDING_RESOURCES.put(AUX_7_0, Arrays.asList(PLAT_CUISINÉ, PAIN, PLAT_CUISINÉ_NOURRITURE));

        // Atelier de Maçonnerie - AUX_8_X
        AUX_BUILDING_RESOURCES.put(AUX_8_0, Arrays.asList(PIERRE_TAILLÉE));
        AUX_BUILDING_RESOURCES.put(AUX_8_1, Arrays.asList(PIERRE_TAILLÉE, BÂTIMENTS));

        // Chapelle/Église/Cathédrale - AUX_9_X
        AUX_BUILDING_RESOURCES.put(AUX_9_0, Arrays.asList());
        AUX_BUILDING_RESOURCES.put(AUX_9_1, Arrays.asList(LIVRES));
        AUX_BUILDING_RESOURCES.put(AUX_9_2, Arrays.asList(LIVRES, BIJOUX)); // Cathédrale avec verre

        // Serres de Culture - AUX_10_X
        AUX_BUILDING_RESOURCES.put(AUX_10_0, Arrays.asList(HERBES_MÉDICINALES, FLEURS));
        AUX_BUILDING_RESOURCES.put(AUX_10_1, Arrays.asList(HERBES_MÉDICINALES, FLEURS, EPICES));

        // Tribunal - AUX_11_0
        AUX_BUILDING_RESOURCES.put(AUX_11_0, Arrays.asList());

        // Ruches et Atelier d'Apiculteur - AUX_12_X
        AUX_BUILDING_RESOURCES.put(AUX_12_0, Arrays.asList()); // TODO: ajouter miel
        AUX_BUILDING_RESOURCES.put(AUX_12_1, Arrays.asList());

        // Péage - AUX_13_0
        AUX_BUILDING_RESOURCES.put(AUX_13_0, Arrays.asList()); // Génère de l'or, pas des ressources

        // Cave d'affinage - AUX_14_X
        AUX_BUILDING_RESOURCES.put(AUX_14_0, Arrays.asList(FROMAGE_À_PÂTE_FRAÎCHE));
        AUX_BUILDING_RESOURCES.put(AUX_14_1, Arrays.asList(
                FROMAGE_À_PÂTE_FRAÎCHE,
                FROMAGES_À_PÂTE_MOLLE_ET_CROÛTE_FLEURIE,
                FROMAGES_À_PÂTE_MOLLE_ET_CROÛTE_LAVÉE
        ));

        // Poste Frontière - AUX_15_0
        AUX_BUILDING_RESOURCES.put(AUX_15_0, Arrays.asList());

        // Pressoir - AUX_16_X
        AUX_BUILDING_RESOURCES.put(AUX_16_0, Arrays.asList(VIN));
        AUX_BUILDING_RESOURCES.put(AUX_16_1, Arrays.asList(VIN));

        // Atelier - AUX_17_0
        AUX_BUILDING_RESOURCES.put(AUX_17_0, Arrays.asList(OUTILS, VÊTEMENTS, MEUBLES));
    }


//================================================== MÉTHODES UTILITAIRES ==============================================


    public static List<DATABASE.ResourceType> getResourcesForBuilding(DATABASE.JobBuilding building) {
        if (building instanceof DATABASE.MainBuilding) {
            return MAIN_BUILDING_RESOURCES.getOrDefault(building, Collections.emptyList());
        } else if (building instanceof DATABASE.AuxBuilding) {
            return AUX_BUILDING_RESOURCES.getOrDefault(building, Collections.emptyList());
        }
        return Collections.emptyList();
    }
    private static void addJobToBuildings(String jobName, JobBuilding... buildings) {
        Set<JobBuilding> buildingSet = JOB_TO_BUILDINGS.computeIfAbsent(jobName, k -> new HashSet<>());
        Collections.addAll(buildingSet, buildings);
    }
    public static double calculateProduction(DATABASE.ResourceType resource, int workers, double efficiency) {
    return resource.getBaseValue() * workers * efficiency;
}
    public static double calculateSalaryCost(String jobCategory, int workers) {
    // Utiliser les données de la classe Metier si disponible
    double baseSalary = 0.0; // Par défaut
    return baseSalary * workers;

    }
    public static Set<JobBuilding> getBuildingsForJob(String jobName) {
        return JOB_TO_BUILDINGS.getOrDefault(jobName, Collections.emptySet());
    }
    public static boolean canJobWorkInBuilding(String jobName, DATABASE.JobBuilding building) {
        Set<JobBuilding> buildings = getBuildingsForJob(jobName);
        return buildings.contains(building);
    }
    public static Set<String> getJobsForBuilding(JobBuilding building) {
        return JOB_TO_BUILDINGS.entrySet().stream()
                .filter(entry -> entry.getValue().contains(building))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
    public static JobBuilding getActiveBuildingForJob(String jobName, SafeHexDetails hexDetails) {
        Set<JobBuilding> possibleBuildings = getBuildingsForJob(jobName);

        // Récupérer les bâtiments actuellement construits dans l'hex
        JobBuilding mainBuilding = MainBuilding.values()[hexDetails.getMainBuildingIndex()];
        JobBuilding auxBuilding = AuxBuilding.values()[hexDetails.getAuxBuildingIndex()];
        JobBuilding fortBuilding = FortBuilding.values()[hexDetails.getFortBuildingIndex()];

        List<JobBuilding> constructedBuildings = List.of(mainBuilding, auxBuilding, fortBuilding);

        // Trouver le bâtiment de tier le plus élevé parmi ceux construits et compatibles
        return possibleBuildings.stream()
                .filter(constructedBuildings::contains)
                .max((b1, b2) -> {
                    int tier1 = getBuildingTier(b1);
                    int tier2 = getBuildingTier(b2);
                    return Integer.compare(tier1, tier2);
                })
                .orElse(null);
    }
    private static int getBuildingTier(JobBuilding building) {
        if (building instanceof MainBuilding mb) return mb.getMainTier();
        if (building instanceof AuxBuilding ab) return ab.getAuxTier();
        if (building instanceof FortBuilding fb) return fb.getFortTier();
        return 0;
    }
    public static void advanceTurn(int newTurn) {
        if (newTurn != Workers.currentTurn) {
            Workers.currentTurn = newTurn;
            Workers.randomSetSalary.setSeed(newTurn * 12345L); // Seed predictible basé sur le tour

            // Recalculer toutes les valeurs pour ce tour
            Workers.turnSalaries.clear();
            Workers.turnFoodConsumption.clear();

            for (Workers job : Workers.values()) {
                if  (job.getBaseSalary() == 0) {
                    // Variation de ±20% autour de la valeur de base
                    double salaryVariation = 0.8 + (Workers.randomSetSalary.nextDouble() * 0.4);
                    Workers.turnSalaries.put(job, (job.baseSalary+1) * salaryVariation);
                }
            }
        }
    }
    public static double getSalaryForJob(String jobName) {
        Workers jobType = findJobTypeByName(jobName);
        return jobType != null ? jobType.getCurrentSalary() : 1.0;
    }
    private static Workers findJobTypeByName(String jobName) {
        return Arrays.stream(Workers.values())
                .filter(job -> job.getJobName().equals(jobName))
                .findFirst()
                .orElse(null);
    }
    private static void addMetier(Map<String, Metier> map, String type, String emploi, double salaire, double nourriture) {
        Set<JobBuilding> buildings = getBuildingsForJob(emploi);
        JobBuilding workbuilding = buildings.isEmpty()
                ? generateWorkbuilding(type, emploi)
                : buildings.iterator().next();

        String iconID = (workbuilding != null) ? workbuilding.getTag() : "unknown";
        map.put(emploi, new Metier(type, emploi, salaire, nourriture, workbuilding, iconID));
    }
    private static JobBuilding generateWorkbuilding(String type, String emploi) {
        boolean placeForteOverride = false;

        if (placeForteOverride && (
                type.equals("Bureau d'Intendance") ||
                        type.equals("Etude") ||
                        type.equals("Diplomatie") ||
                        type.equals("Place-Forte") ||
                        type.equals("Poing de Décimus"))

        ) {

            return MainBuilding.MAIN_25_0;
        }

        String lowerEmploi = emploi.toLowerCase();

        // Search all JobBuildings: Java.Main, Aux, Fort
        for (MainBuilding b : MainBuilding.values()) {
            if (b.getBuildName().toLowerCase().contains(lowerEmploi)) return b;
        }
        for (AuxBuilding b : AuxBuilding.values()) {
            if (b.getBuildName().toLowerCase().contains(lowerEmploi)) return b;
        }
        for (FortBuilding b : FortBuilding.values()) {
            if (b.getBuildName().toLowerCase().contains(lowerEmploi)) return b;
        }

        // Fallback by type-specific guess
        return switch (type) {
            case "Transformation" -> AuxBuilding.AUX_17_0;
            case "Récolte" -> MainBuilding.MAIN_11_1;
            case "Production" -> AuxBuilding.AUX_17_0;
            case "Religieux" -> MainBuilding.MAIN_13_1;
            case "Utilitaire" -> MainBuilding.MAIN_1_0;
            case "Artisan" -> AuxBuilding.AUX_6_0;
            case "Bureau d'Intendance" -> MainBuilding.MAIN_1_0;
            case "Etude" -> MainBuilding.MAIN_1_0;
            case "Diplomatie" -> MainBuilding.MAIN_1_0;
            case "Poing de Décimus" -> MainBuilding.MAIN_1_0;
            case "Magie" -> MainBuilding.MAIN_1_0;
            case "Info" -> MainBuilding.MAIN_1_0;
            default ->

                // Final fallback
                    null;
        };

    }
    public static void showMetiersInTable(Map<String, Metier> metiers) {
        JFrame frame = new JFrame("Metiers");
        String[] columns = {"Type", "Emploi", "Salaire", "Nourriture", "Workbuilding", "IconID"};

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 2 || columnIndex == 3) return Double.class;
                return String.class;
            }
        };

        JTable table = new JTable(model);
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        for (Metier metier : metiers.values()) {
            model.addRow(new Object[]{
                    metier.type,
                    metier.emploi,
                    metier.salaire,
                    metier.demandeNourriture,
                    metier.workbuilding,
                    metier.iconID
            });
        }


        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // 🔍 Add search field
        JTextField searchField = new JTextField(20);
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateFilter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateFilter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateFilter(); }

            private void updateFilter() {
                String text = searchField.getText();
                if (text.trim().length() == 0) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text)); // (?i) = case-insensitive
                }
            }
        });

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("🔎 Search: "), BorderLayout.WEST);
        panel.add(searchField, BorderLayout.CENTER);

        frame.setAlwaysOnTop(true);
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.NORTH);
        frame.add(new JScrollPane(table), BorderLayout.CENTER);
        frame.setSize(600, 350);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setVisible(true);
    }
    public static String getBuildNameFromJobBuilding(JobBuilding jb) {
        if (jb instanceof MainBuilding mb) return mb.getBuildName();
        if (jb instanceof AuxBuilding ab) return ab.getBuildName();
        if (jb instanceof FortBuilding fb) return fb.getBuildName();
        return "unknown";
    }
    private static String getTagFromJobBuilding(JobBuilding jb) {
        if (jb instanceof MainBuilding mb) return mb.getMainTag();
        if (jb instanceof AuxBuilding ab) return ab.getAuxTag();
        if (jb instanceof FortBuilding fb) return fb.getFortTag();
        return "unknown";
    }

    public static class Metier {
        public final JobBuilding workbuilding;
        public final String iconID;
        public final String type;
        public final String emploi;
        public final double salaire;
        public final double demandeNourriture;

        public Metier(String type, String emploi, double salaire, double demandeNourriture, JobBuilding workbuilding, String iconID) {
            this.type = type;
            this.emploi = emploi;
            this.salaire = salaire;
            this.demandeNourriture = demandeNourriture;
            this.workbuilding = workbuilding;
            this.iconID = iconID;
        }

        @Override
        public String toString() {
            String buildingTag = (workbuilding != null) ? workbuilding.getTag() : "unknown";
            return String.format("%s (%s): %.2fpo, Nourriture: %.2f, Bâtiment: %s, Icon: %s",
                    emploi, type, salaire, demandeNourriture, buildingTag, iconID);
        }
    }

    public static BuildingProduction getProductionData(JobBuilding building) {
        return BUILDING_PRODUCTION_DATA.get(building);
    }
    public static double calculateActualProduction(JobBuilding building, ResourceType resource, int workers, double efficiency) {
        BuildingProduction data = getProductionData(building);
        if (data == null) return 0.0;

        // Check if resource is valid for this building
        boolean validResource = Arrays.asList(data.possibleResources).contains(resource);
        if (!validResource) return 0.0;

        // Cap workers at building maximum
        int effectiveWorkers = Math.min(workers, data.maxWorkers);

        return resource.getBaseValue() * data.baseProductionRate * effectiveWorkers * efficiency;
    }
    public static double getFoodConsumptionForJob(String jobName) {
        Workers jobType = findJobTypeByName(jobName);
        return jobType != null ? jobType.getCurrentFoodConsumption() : 0.6;
    }
    public static double calculateTotalFoodConsumption(Map<String, Integer> jobCounts) {
        double totalConsumption = 0.0;
        for (Map.Entry<String, Integer> entry : jobCounts.entrySet()) {
            String job = entry.getKey();
            int count = entry.getValue();
            totalConsumption += getFoodConsumptionForJob(job) * count;
        }
        return totalConsumption;
    }

    public interface ProductionStrategy {
        double calculateProduction(DATABASE.ResourceType resource, int workers, double efficiency);
    }
    public class LinearProductionStrategy implements ProductionStrategy {
        @Override
        public double calculateProduction(DATABASE.ResourceType resource, int workers, double efficiency) {
            return resource.getBaseValue() * workers * efficiency;
        }
    }
    public class DiminishingReturnsStrategy implements ProductionStrategy {
        @Override
        public double calculateProduction(DATABASE.ResourceType resource, int workers, double efficiency) {
            double base = resource.getBaseValue() * efficiency;
            return base * (workers - (workers * workers * 0.01)); // Rendements décroissants
        }
    }
    public class ProductionStrategyFactory {
        public ProductionStrategy getStrategy(DATABASE.ResourceType resource) {
            if (resource.getCategory().equals("Minerais")) {
                return new DiminishingReturnsStrategy(); // Mines s'épuisent
            }
            return new LinearProductionStrategy(); // Production normale
        }
    }
    public static final Map<MainBuilding, List<ResourceType>> MAIN_BUILDING_RESOURCES = new HashMap<>();
    public static final Map<AuxBuilding, List<ResourceType>> AUX_BUILDING_RESOURCES = new HashMap<>();

// ========== MÉTHODE DE VALIDATION (TEMPORAIRE) ==========

    public static void validateResourceConfiguration() {
        System.out.println("=== VALIDATION DE LA CONFIGURATION DES RESSOURCES ===");

        int totalMainConfigured = 0;
        int totalAuxConfigured = 0;

        // Test des bâtiments principaux
        System.out.println("\n--- BÂTIMENTS PRINCIPAUX ---");
        for (MainBuilding building : MainBuilding.values()) {
            List<ResourceType> resources = getResourcesForBuilding(building);
            System.out.println(String.format("%-30s -> %d ressources: %s",
                    building.getBuildName(),
                    resources.size(),
                    resources.isEmpty() ? "AUCUNE" : resources.stream()
                            .map(ResourceType::getName)
                            .limit(3)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("")
            ));
            if (!resources.isEmpty()) totalMainConfigured++;
        }

        // Test des bâtiments auxiliaires
        System.out.println("\n--- BÂTIMENTS AUXILIAIRES ---");
        for (AuxBuilding building : AuxBuilding.values()) {
            List<ResourceType> resources = getResourcesForBuilding(building);
            System.out.println(String.format("%-30s -> %d ressources: %s",
                    building.getBuildName(),
                    resources.size(),
                    resources.isEmpty() ? "AUCUNE" : resources.stream()
                            .map(ResourceType::getName)
                            .limit(3)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("")
            ));
            if (!resources.isEmpty()) totalAuxConfigured++;
        }

        System.out.println("\n--- RÉSUMÉ ---");
        System.out.println("Bâtiments principaux configurés: " + totalMainConfigured + "/" + MainBuilding.values().length);
        System.out.println("Bâtiments auxiliaires configurés: " + totalAuxConfigured + "/" + AuxBuilding.values().length);

        // Vérifier les ressources manquantes
        System.out.println("\n--- RESSOURCES POTENTIELLEMENT MANQUANTES ---");
        Set<String> missingResources = new HashSet<>();
        missingResources.add("Argile");
        missingResources.add("Pierre");
        missingResources.add("Goudron");
        missingResources.add("Miel");

        for (String missing : missingResources) {
            System.out.println("⚠️  ResourceType manquant: " + missing);
        }
    }

// ========== MÉTHODE DE NETTOYAGE ==========

    /**
     * @deprecated Utiliser getResourcesForBuilding() à la place
     * Cette Map sera supprimée dans une version future
     */
    @Deprecated
    public static final Map<String, ResourceType[]> BUILDING_RESOURCES = Map.of(
            // Garder temporairement pour compatibilité, puis supprimer
            "MAIN", new ResourceType[]{},
            "AUX", new ResourceType[]{}
    );


    public static class Monetary         {
        public int PlatinumCurrency;
        public int GoldCurrency;
        public int SilverCurrency;
        public int CopperCurrency;
        public int salaireTotal;

    }
    static { initializeJobBuildingRelations();
        initializeMainBuildingResources();
        initializeAuxBuildingResources();
    }

}


